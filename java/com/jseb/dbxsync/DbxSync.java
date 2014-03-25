package com.jseb.dbxsync;

import com.jseb.dbxsync.commands.DbxSyncCommand;
import com.jseb.dbxsync.dev.Keys;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.Server;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

import com.dropbox.core.*;

public class DbxSync extends JavaPlugin {
	private static final Logger LOGGER = Logger.getLogger("DbxSync");
	public static DbxWebAuthNoRedirect webAuth;
	public static DbxClient client;

	public void onEnable() {
		initDropboxAuth(getDataFolder());
		getCommand("dbxsync").setExecutor(new DbxSyncCommand(this));
	}

	public void onDisable() {
		
	}

	public static void initDropboxAuth(File dataFolder) {
		try {
			File dataFile = new File(dataFolder, "accesstoken.yml");
			DbxRequestConfig config = new DbxRequestConfig("DbxSync/1.0", Locale.getDefault().toString());

			if (dataFile.exists() && YamlConfiguration.loadConfiguration(dataFile).contains("auth_token")) {
				client = new DbxClient(config, YamlConfiguration.loadConfiguration(dataFile).getString("auth_token"));
				LOGGER.info("[WS] dropbox successfully linked");
			} else {
				dataFile.getParentFile().mkdirs();
				dataFile.createNewFile();
				webAuth = new DbxWebAuthNoRedirect(config, new DbxAppInfo(Keys.APP_KEY, Keys.APP_SECRET));
	
				LOGGER.info("Authorize via: " + webAuth.start());
				LOGGER.info("then finish with /ws link <code>");
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}

	public static void finishDropboxAuth(final String code, final File dataFolder) {
		new Thread(new Runnable() {
			public void run() {
				try {
					DbxAuthFinish authFinish = webAuth.finish(code);
					DbxSync.client = new DbxClient(new DbxRequestConfig("DbxSync/1.0", Locale.getDefault().toString()), authFinish.accessToken);
					LOGGER.info("[WS] dropbox successfully linked");

					YamlConfiguration dataFile = YamlConfiguration.loadConfiguration(new File(dataFolder, "accesstoken.yml"));
					dataFile.set("auth_token", authFinish.accessToken);
					dataFile.save(new File(dataFolder, "accesstoken.yml"));
				} catch (DbxException | IOException e) {
					LOGGER.info("[WS] error linking: " + e.getMessage());	
				}
			}
		}).start();
	}
}