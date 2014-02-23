package com.jseb.worldsync.tasks;

import com.jseb.worldsync.WorldSync;

import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import com.dropbox.client2.exception.DropboxException;
import com.dropbox.client2.exception.DropboxUnlinkedException;
import com.dropbox.client2.exception.DropboxPartialFileException;
import com.dropbox.client2.session.WebAuthSession;
import com.dropbox.client2.ProgressListener;
import com.dropbox.client2.DropboxAPI;

public class BackupTask implements Runnable {
	public static WorldSync plugin;
	public static DropboxAPI.ChunkedUploader uploader;
	public boolean isActive;
	public boolean verbose;

	public BackupTask(WorldSync plugin, boolean verbose) {
		this.plugin = plugin;
		this.isActive = false;
		this.verbose = verbose;
	}

	@Override
	public void run() {
		this.isActive = true;
		
		try {
			WebAuthSession serverSession = new WebAuthSession(plugin.appKey, plugin.ACCESS_TYPE, plugin.accessToken);
			DropboxAPI<?> api = new DropboxAPI<WebAuthSession>(serverSession);
			String localPath = getMostRecentBackup();

			if (localPath == "") throw new IOException("no recent backups found");

			File file = new File(localPath);
			uploader = api.getChunkedUploader(new FileInputStream(file), file.length());
			ProgressListener plistener = new ProgressListener() {
				public void onProgress(long bytes, long total) {
					//if (bytes / 1000)
					if (verbose) System.out.println(bytes/1000 + " of " + total/1000 + " KB transfered (" + new DecimalFormat("#.00").format(((double)bytes/(double)total) * 100) + "%)");
				}
			};

			while(!uploader.isComplete()) uploader.upload(plistener);
			uploader.finish(file.getName(), null);
			System.out.println("[WS] successfully backed up world.");
		} catch (DropboxException e) {
			if (e instanceof DropboxUnlinkedException) System.out.println("[WS] make sure you've linked app to your DropBox");
			else if (!(e instanceof DropboxPartialFileException)) System.out.println("[WS] Dropbox error while backing up: " + e.getMessage());
		} catch (FileNotFoundException e) {
			System.out.println("[WS] File IO error while backing up: " + e.getMessage());
		} catch (IOException e) {
			System.out.println("[WS] IO error while backing up: " + e.getMessage());
		} finally {
			this.isActive = false;
		}
	}

	public void cancel() {
		if (uploader != null) if (uploader.getActive()) uploader.abort();

		this.isActive = false;
		System.out.println("[WS] backup cancelled");
	}

	public String getMostRecentBackup() {
		File lastModified = null;
		return "/Users/tills13/Desktop/craftbukkit.jar";
		/*for (File file : new File("/root/McMyAdmin/Backups/").listFiles()) {
			if (lastModified == null) lastModified = file;
			else {
				if (lastModified.lastModified() < file.lastModified()) lastModified = file;
			}
		}

		return lastModified == null ? "" : lastModified.getAbsolutePath();*/
	}
}