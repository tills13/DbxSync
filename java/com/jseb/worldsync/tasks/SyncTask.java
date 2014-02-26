package com.jseb.worldsync.tasks;

import com.jseb.worldsync.WorldSync;

import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.World;

import com.dropbox.core.*;

public class SyncTask implements Runnable {
	private final static Logger LOGGER = Logger.getLogger("SyncTask");
	public static WorldSync plugin;
	public World world;	
	public boolean isActive;
	public boolean verbose;

	public SyncTask(WorldSync plugin, World world) {
		this(plugin, world, false);
	}

	public SyncTask(WorldSync plugin, World world, boolean verbose) {
		this.plugin = plugin;
		this.world = world;
		this.isActive = false;
		this.verbose = verbose;
	}

	@Override
	public void run() {
		try {
			this.isActive = true;

			File localFile = getBackup(this.world);
			if (localFile == null) throw new IOException("world doesn't exist!");
			DbxClient.Uploader uploader = plugin.client.startUploadFileChunked(getRemoteFile(localFile), DbxWriteMode.force(), localFile.length());
			FileInputStream in = new FileInputStream(localFile);

			int data, bytes = 0; 
			while (this.isActive && (data = in.read()) != -1) {
				uploader.getBody().write(data);
				if (verbose && ((++bytes % (localFile.length() / 15)) == 0)) LOGGER.info("[WS] " + (bytes / 1000) + " KB uploaded (" + (int)((double)bytes / localFile.length() * 100) + "%)");
			}

			if (this.isActive) {
				uploader.finish();
				LOGGER.info("[WS] upload successful");
			} else {
				uploader.close();
				LOGGER.info("[WS] upload canceled");
			}
		} catch (IOException | DbxException e) {
			LOGGER.info("[WS] something went wrong during upload...");
			LOGGER.info("[WS] " + e.getMessage());
		} finally {
			this.isActive = false;
		}	
	}

	public void cancel() {
		this.isActive = false;
	}

	public String getRemoteFile(File file) {
		return "/backups/" + file.getName();
	}

	public File getBackup(World world) throws IOException {
		File file = new File(plugin.getDataFolder() + File.separator + "backups" + File.separator + world.getName() + "_backup.zip");
		if (file == null) throw new IOException("no backups found... create a backup first.");

		return file;
	}
}