package com.jseb.worldsync.commands;

import com.jseb.worldsync.WorldSync;
import com.jseb.worldsync.helpers.ZipHelper;
import com.jseb.worldsync.tasks.SyncTask;
import com.jseb.worldsync.tasks.DownloadTask;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.entity.Player;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import com.dropbox.core.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.logging.Logger;

public class WorldSyncCommand implements CommandExecutor {
	private final static Logger LOGGER = Logger.getLogger("WorldSyncCommand");
	private WorldSync plugin;
	public SyncTask syncTask;
	public Thread restoreTask;

	public WorldSyncCommand(WorldSync plugin) {
		this.plugin = plugin;
		this.syncTask = null;
	}

	@Override
	public boolean onCommand(final CommandSender sender, Command cmd, String label, final String[] args) {
		if (args.length == 0) helpSyntax(sender);
		else {
			if (args[0].equalsIgnoreCase("link")) {
				if (plugin.client != null) {
					if (sender instanceof Player) {
						sender.sendMessage("[WS] already authenticated/linked");
						sender.sendMessage("[WS] to re-auth, delete \"accesstoken.yml\"");
					} else  LOGGER.info("[WS] already authenticated, delete \"accesstoken.yml\" to re-auth");
				} else {
					if (plugin.webAuth == null) sender.sendMessage("[WS] you need to start the auth session");
					else WorldSync.finishDropboxAuth(args[1], plugin.getDataFolder());
				}
			} else if (args[0].equalsIgnoreCase("backup")) { // create a backup
				if (args.length == 2) ZipHelper.zip(Bukkit.getServer().getWorld(args[1]).getWorldFolder(), new File(plugin.getDataFolder() + File.separator + "backups" + File.separator + Bukkit.getServer().getWorld(args[1]).getName() + "_backup.zip"));
				else for (World world : plugin.getServer().getWorlds()) ZipHelper.zip(world.getWorldFolder(), new File(plugin.getDataFolder() + File.separator + "backups" + File.separator + world.getName() + "_backup.zip"));
			} else if (args[0].equalsIgnoreCase("sync")) { // sync to dropbox
				if (this.restoreTask != null && this.restoreTask.isAlive()) sender.sendMessage("[WS] restore currently running, please wait until restore complete");
				else {
					this.syncTask = new SyncTask(plugin, args.length == 2 ? Bukkit.getServer().getWorld(args[1]) : ((Player) sender).getWorld(), true); // change to sync
					plugin.getServer().getScheduler().runTaskAsynchronously(plugin, this.syncTask);
				}
			} else if (args[0].equalsIgnoreCase("cancel")) { // cancel current sync
				if (this.syncTask != null && this.syncTask.isActive) syncTask.cancel();
				else if (this.restoreTask != null && this.restoreTask.isAlive()) restoreTask.interrupt();
				else {
					if (sender instanceof Player) sender.sendMessage("[WS] sync/restore is not currently running");
					else LOGGER.info("[WS] sync/restore is not currently running");
				}	
			} else if (args[0].equalsIgnoreCase("restore")) { // restore world from dropbox
				if (this.syncTask != null && this.syncTask.isActive) sender.sendMessage("[WS] sync currently running, please wait until sync complete");
				else {
					final WorldSync mPlugin = plugin;
					this.restoreTask = new Thread(new Runnable() {
						public void run() {
							World mWorld = Bukkit.getServer().getWorld(args[1]);
							if (mWorld != null) {
								try {
									Bukkit.getServer().broadcastMessage("[WS] world restore in progress, stand by...");
									Bukkit.getServer().unloadWorld(mWorld, false);

									LOGGER.info("[WS] downloading zip...");
									new DownloadTask(plugin, args.length == 2 ? Bukkit.getServer().getWorld(args[1]) : ((Player) sender).getWorld(), true).run();

									LOGGER.info("[WS] unzipping result...");
									ZipHelper.unzip(new File(mPlugin.getDataFolder(), File.separator + "downloaded_backups" + File.separator + args[1] + "_backup.zip"), new File(mPlugin.getDataFolder(), File.separator + "downloaded_backups" + File.separator + args[1]));

									LOGGER.info("[WS] swapping world...");

									Path oldWorldFolder = FileSystems.getDefault().getPath(mPlugin.getDataFolder().getPath() + File.separator + "downloaded_backups" + File.separator + mWorld.getName());
									Path newWorldFolder = FileSystems.getDefault().getPath(mWorld.getWorldFolder().getPath());
							
									Files.move(newWorldFolder, oldWorldFolder, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
									Bukkit.getServer().createWorld(new WorldCreator(mWorld.getName()));
									LOGGER.info("[WS] swapped and reloaded world");
								} catch (IOException e) {

								}
							} else System.out.println("asdasd");
						}
					});

					restoreTask.start();
				}
			} else if (args[0].equalsIgnoreCase("list")) { // list of remote and local backups
				sender.sendMessage("[WS] local backups");
				File backupDir = new File(plugin.getDataFolder(), "backups");
				if (backupDir.exists()) {
					for (File file : backupDir.listFiles()) sender.sendMessage("    " + file.getName());
				} else sender.sendMessage("    no local backups; 'ws backup <world>' to perform a backup");

				final WorldSync mPlugin = plugin;
				new Thread(new Runnable() {
					public void run() {
						try {
							if (mPlugin.client != null) {
								DbxEntry.WithChildren listing = mPlugin.client.getMetadataWithChildren("/backups");
								sender.sendMessage("[WS] remote backups");
								if (listing != null) {
									for (DbxEntry child : listing.children) sender.sendMessage("    " + child.name + " modified " + child.asFile().lastModified);	
								} else sender.sendMessage("    no remote backups; 'ws sync <world>' to sync a backup");
							} else sender.sendMessage("[WS] plugin needs to be linked to display remote backups");
						} catch (DbxException e) {
							sender.sendMessage("[WS] something went wrong while displaying remote backups");
							LOGGER.info("[WS] something went wrong while displaying remote backups: " + e.getMessage());
						}
					}
				}).start();
			} else if (args[0].equalsIgnoreCase("session")) {
				final WorldSync mPlugin = plugin;
				new Thread(new Runnable() {
					public void run() {
						try {
							if (mPlugin.client != null) {
								DbxAccountInfo acctInfo = mPlugin.client.getAccountInfo();
								sender.sendMessage("[WS] session info");
								sender.sendMessage("    user: " + acctInfo.displayName);
								sender.sendMessage("    user id: " + acctInfo.userId);
								sender.sendMessage("    quota: " + acctInfo.quota);
								sender.sendMessage("    token: " + mPlugin.client.getAccessToken());
							} else sender.sendMessage("[WS] plugin needs to be linked to display remote backups");
						} catch (DbxException e) {
							sender.sendMessage("[WS] something went wrong while displaying remote backups");
							LOGGER.info("[WS] something went wrong while displaying remote backups: " + e.getMessage());
						}
					}
				}).start();
			} else helpSyntax(sender);
		}

		return true;
	}

	public void helpSyntax(CommandSender sender) {
		sender.sendMessage("[WS] command overview");
		sender.sendMessage("    1. ws link <code> - link the app to dropbox");
		sender.sendMessage("    2. ws backup <world> - create backup of specified world or all worlds");
		sender.sendMessage("    3. ws sync <world> - sync world specified or current world");
		sender.sendMessage("    4. ws cancel - cancels sync in progress");
		sender.sendMessage("    5. ws restore - restores specified world or all worlds");
		sender.sendMessage("    6. ws session - information about the current session");
		sender.sendMessage("    7. ws list - list local and remote backups");
	}
}