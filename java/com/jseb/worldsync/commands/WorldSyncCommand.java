package com.jseb.worldsync.commands;

import com.jseb.worldsync.WorldSync;
import com.jseb.worldsync.tasks.BackupTask;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

import com.dropbox.client2.exception.DropboxException;

import java.util.Calendar;
import java.util.List;

public class WorldSyncCommand implements CommandExecutor {
	private WorldSync plugin;
    public BackupTask backupTask;
    public List<BukkitTask> backups;

	public WorldSyncCommand(WorldSync plugin) {
		this.plugin = plugin;
        this.backupTask = null;
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (args.length == 0) return true;

        if (args[0].equalsIgnoreCase("link")) {
            try {
                if (plugin.accessToken != null) {
                    System.out.println("[WS] already authenticated/linked");
                    System.out.println("[WS] to re-auth, delete \"accesstoken.dat\"");
                    return true;
                }
                if (plugin.was == null) return false;

                plugin.uid = plugin.was.retrieveWebAccessToken(plugin.info.requestTokenPair);
                plugin.accessToken = plugin.was.getAccessTokenPair();
            } catch (DropboxException e) {
                System.out.println("[WS] error linking: " + e.getMessage());
                return false;
            }
            System.out.println("[WS] successfully linked");
        } else if (args[0].equalsIgnoreCase("backup")) {
            if (args.length > 2) return false; //backup with delay

            if (this.backupTask != null) {
                if (this.backupTask.isActive) {
                    if (args.length == 2) {
                        if (!args[1].equalsIgnoreCase("cancel")) {
                            System.out.println("[WS] backup in progress");
                            return true;
                        } else {
                            System.out.println("[WS] attempting to cancel...");
                        }
                    } else {
                        System.out.println("[WS] backup in progress");
                        return true;
                    }
                }
            }

            if (args.length == 1) {
                backupTask = new BackupTask(plugin);
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, backupTask);
                return true;
            }

            if (args[1].equalsIgnoreCase("now")) {
                backupTask = new BackupTask(plugin);
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, backupTask);
            } else if (args[1].equalsIgnoreCase("tonight")) { //backup at midnight
                backupTask = new BackupTask(plugin);
                Calendar cal = Calendar.getInstance();
                int curHour = cal.get(Calendar.HOUR);
                int delay = (cal.get(Calendar.AM_PM) == 0) ? 24 - curHour : 12 - curHour;
                System.out.println("[WS] backup will begin in " + delay + " hours");
                plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, backupTask, delay * 60 * 20 * 60);
            } else if (args[1].equalsIgnoreCase("cancel")) {
                if (backupTask.isActive) {
                    backupTask.cancel();
                } else {
                    backupTask = null;
                    System.out.println("[WS] backup is not currently running");
                }
            } else {
                try {
                    backupTask = new BackupTask(plugin);
                    int delay = Integer.parseInt(args[1]);
                    System.out.println("[WS] backup will begin in " + delay + " hours");
                    plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, backupTask, delay * 20 * 60 * 60);
                } catch (NumberFormatException e) {
                    backupTask = null;

                    System.out.println("[WS] unable to parse arguments");
                    return true;
                }
            }
        } else if (args[0].equalsIgnoreCase("restore")) {
        } else if (args[0].equalsIgnoreCase("info")) {
            sender.sendMessage("[WS] connection info: ");
            sender.sendMessage("uid: " + (plugin.accessToken == null ? "not connected" : plugin.uid));
            sender.sendMessage("key: " + (plugin.accessToken == null ? "not connected" : plugin.accessToken.key));
            sender.sendMessage("secret: " + (plugin.accessToken == null ? "not connected" : plugin.accessToken.secret));
            //sender.sendMessage(": " + plugin.accessToken == null ? "not connected" : plugin.accessToken.secret);
        }

        return true;
    }
}