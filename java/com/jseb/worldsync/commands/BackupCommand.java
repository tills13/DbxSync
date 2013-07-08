package com.jseb.worldsync.commands;

import com.jseb.worldsync.WorldSync;
import com.jseb.worldsync.tasks.BackupTask;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

import java.util.Calendar;

public class BackupCommand implements CommandExecutor {
	private WorldSync plugin;
    public BukkitTask task;
    public BackupTask backupTask;

	public BackupCommand(WorldSync plugin) {
		this.plugin = plugin;
        this.task = null;
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 1) return false; //backup with delay

        if (task != null) {
            if (plugin.getServer().getScheduler().isCurrentlyRunning(task.getTaskId())) {
                if (args.length == 1) {
                    if (!args[0].equalsIgnoreCase("cancel")) {
                        System.out.println("[WS] backup in progress");
                        return true;
                    }
                }
            }
        }

        if (args.length == 0) {
            backupTask = new BackupTask(plugin);
            task = plugin.getServer().getScheduler().runTaskAsynchronously(plugin, backupTask);
            return true;
        }

		if (args[0].equalsIgnoreCase("now")) {
            backupTask = new BackupTask(plugin);
            task = plugin.getServer().getScheduler().runTaskAsynchronously(plugin, backupTask);
		} else if (args[0].equalsIgnoreCase("tonight")) { //backup at midnight
            backupTask = new BackupTask(plugin);
            Calendar cal = Calendar.getInstance();
            int curHour = cal.get(Calendar.HOUR);
            System.out.println(cal.get(Calendar.AM_PM));
            int delay = (cal.get(Calendar.AM_PM) == 0) ? 24 - curHour : 12 - curHour;
            System.out.println("[WS] backup will begin in " + delay + " hours");
            task = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, backupTask, delay * 60 * 20 * 60);
		} else if (args[0].equalsIgnoreCase("cancel")) {
            if (plugin.getServer().getScheduler().isCurrentlyRunning(task.getTaskId())) backupTask.cancel();
            else task = null;

            System.out.println("[WS] task cancelled");
        } else {
            try {
                backupTask = new BackupTask(plugin);
                int delay = Integer.parseInt(args[0]);
                System.out.println("[WS] backup will begin in " + delay + " hours");
                task = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, backupTask, delay * 20 * 60 * 60);
            } catch (NumberFormatException e) {
                backupTask = null;

                System.out.println("[WS] unable to parse arguments");
                return true;
            }
		}

        return true;
    }
}