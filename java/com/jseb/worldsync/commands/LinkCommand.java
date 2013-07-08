package com.jseb.worldsync.commands;

import com.jseb.worldsync.WorldSync;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.ChatColor;

import com.dropbox.client2.exception.DropboxException;

public class LinkCommand implements CommandExecutor {
	WorldSync plugin;

	public LinkCommand(WorldSync plugin) {
		this.plugin = plugin;
	}

	@Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
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
		return true;
    }
}