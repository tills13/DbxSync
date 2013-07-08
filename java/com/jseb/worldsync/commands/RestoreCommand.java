package com.jseb.worldsync.commands;

import com.jseb.worldsync.WorldSync;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class RestoreCommand implements CommandExecutor {
	private WorldSync plugin;

	public RestoreCommand(WorldSync plugin) {
		this.plugin = plugin;
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    	if (args.length == 0) return plugin.util.doRestore();
    	else if (args.length == 1) { //backup with delay
    		if (args[0].equalsIgnoreCase("now")) return plugin.util.doRestore();
    		else if (args[0].equalsIgnoreCase("tonight")) { //backup at 4am

    		} else if (args[0].equalsIgnoreCase("asdj")) { //check for isANumber()

    		}
    	}

        return true;
    }
}