package com.jseb.worldsync.helpers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipHelper {
	private final static Logger LOGGER = Logger.getLogger("ZipHelper");

	public static void zip(final File source, final File dest) {
		new Thread(new Runnable() {
			public void run() {
				try {
					if (!dest.getParentFile().exists()) dest.getParentFile().mkdirs();
					if (!dest.exists()) dest.createNewFile();	

					ZipOutputStream out = new ZipOutputStream(new FileOutputStream(dest));
					zipDir(source, "", out);
					out.close();

					LOGGER.info("backup successfully created at " + dest.getName());
				} catch (IOException e) { LOGGER.info("[WS] something went wrong creating a backup zip :( " + e.getMessage()); }
			}
		}).start();
	}

	private static void zipDir(File dir, String path, ZipOutputStream out) throws IOException {
		byte[] buffer = new byte[1024];

		for (File file : dir.listFiles()) {
			if (file.isDirectory()) zipDir(file, path + File.separator + file.getName(), out);
			else {
				LOGGER.info("[WS] zipping: " + path + File.separator + file.getName());
				FileInputStream in = new FileInputStream(file);
				out.putNextEntry(new ZipEntry(path + File.separator + file.getName()));

				int length;
            	while((length = in.read(buffer)) > 0) out.write(buffer, 0, length);

            	out.closeEntry();
            	in.close();
			}
		}
	} 

	public static void unzip(File source, File dest) throws IOException {
		if (!dest.exists()) dest.mkdirs();

		unzipDir(source.getPath(), dest.getPath());
		LOGGER.info("backup successfully unzipped at " + dest.getName());
	}

	public static void unzipAsync(final File source, final File dest) {
		new Thread(new Runnable() {
			public void run() {
				try {	
					if (!dest.exists()) dest.mkdirs();

					unzipDir(source.getPath(), dest.getPath());

					LOGGER.info("backup successfully unzipped at " + dest.getName());
				} catch (IOException e) { LOGGER.info("[WS] something went wrong upzipping :( " + e.getMessage()); }
			}
		}).start();
	}

	private static void unzipDir(String source, String dest) throws IOException {
		ZipFile	zip = new ZipFile(source);
		Enumeration e = zip.entries();
		
		while (e.hasMoreElements()) {
			ZipEntry entry = (ZipEntry) e.nextElement();
			
			
			File file = new File(dest, entry.getName());
			if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
			BufferedInputStream in = new BufferedInputStream(zip.getInputStream(entry));
			FileOutputStream out = new FileOutputStream(new File(dest, entry.getName()));	

			int data;
			while((data = in.read()) != -1) out.write(data);
			in.close();
			out.close();
		}
	} 
}