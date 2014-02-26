package com.jseb.worldsync.tasks;

import com.jseb.worldsync.WorldSync;

import java.io.FileOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.World;

import com.dropbox.core.*;

public class DownloadTask implements Runnable {
	private final static Logger LOGGER = Logger.getLogger("DownloadTask");
	public static WorldSync plugin;
	public World world;	
	public boolean isActive;
	public boolean verbose;

	public DownloadTask(WorldSync plugin, World world) {
		this(plugin, world, false);
	}

	public DownloadTask(WorldSync plugin, World world, boolean verbose) {
		this.plugin = plugin;
		this.world = world;
		this.isActive = false;
		this.verbose = verbose;
	}

	@Override
	public void run() {
		try {
			this.isActive = true;
			DbxClient.Downloader downloader = plugin.client.startGetFile(getRemoteFile(), null);
			FileOutputStream out = new FileOutputStream(getLocalFile());

			int data, bytes = 0;
			long size = downloader.metadata.numBytes; 
			while (this.isActive && ((data = downloader.body.read()) != -1)) {
				if (verbose && (++bytes % (size / 15) == 0)) LOGGER.info("[WS] " + (bytes / 1000) + " KB downloaded (" + (int)((double) bytes / size * 100) + "%)");
				out.write(data);
			}

			if (this.isActive) LOGGER.info("[WS] download successful");
			else LOGGER.info("[WS] download canceled");

			out.close();
			downloader.close();
		} catch (IOException | DbxException e) {
			LOGGER.info("[WS] something went wrong during backup...");
			LOGGER.info("[WS] " + e.getMessage());
		} finally {
			this.isActive = false;
		}	
	}

	public void cancel() {
		this.isActive = false;
	}

	public File getLocalFile() throws IOException {
		File file = new File(plugin.getDataFolder() + File.separator + "downloaded_backups" + File.separator + world.getName() + "_backup.zip");
		if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
		if (!file.exists()) file.createNewFile();

		return file;
	}

	public String getRemoteFile() {
		return "/backups/" + this.world.getName() + "_backup.zip";
	}
}