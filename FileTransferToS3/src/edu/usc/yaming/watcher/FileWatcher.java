package edu.usc.yaming.watcher;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.*;

public class FileWatcher {
	public WatchService watcher;
	public Map<WatchKey, Path> keys;
	
	@SuppressWarnings("unchecked")
	public static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}
	
	public FileWatcher(Path dir) throws Exception {
		this.watcher = FileSystems.getDefault().newWatchService();
		this.keys = new HashMap<WatchKey, Path>();
		
		/* We always scan the folder recursively */
		//System.out.format("Scanning %s ...\n", dir);
		register(dir);
		//System.out.println("Done!");

	}
	
	public void register(Path start) throws IOException {
		Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attr) throws IOException{
				/* Since we want to upload files to S3, 
				 * we only monitor the creation and deletion
				 */
				WatchKey key = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE);
				keys.put(key, dir);
				
				return FileVisitResult.CONTINUE;
			}
		}); 
		
	}
	
	public void processEvents() {
		WatchKey key;
		while (true) {
			try {
				 key = watcher.take();
			} catch(InterruptedException e) {
				return;
			}
				
			Thread thread = new Thread(new Process(watcher, keys, key));
			thread.start();
			System.out.println("New thread id: " + thread.getId() + " running...");
		}
	}
	
	private class Process implements Runnable {
		public WatchService watcher;
		public Map<WatchKey, Path> keys;
		public WatchKey key;
		
		public AmazonS3 s3Client;
		public Process(WatchService w, Map<WatchKey, Path> map, WatchKey wk) {
			watcher = w;
			keys = map;
			key = wk;
			s3Client = new AmazonS3Client(new ProfileCredentialsProvider());
		}
		
		public void run() {
			Path dir = keys.get(key);
			if (dir == null) {
				System.out.println("WatchKey not recognized!!");
				return;
			}
			
			for (WatchEvent<?> event : key.pollEvents()) {
				WatchEvent.Kind kind = event.kind();
				
				if (kind == OVERFLOW) {
					return;
				}
				
				WatchEvent<Path> ev = cast(event);
				Path name = ev.context();
				Path child = dir.resolve(name);
				
				System.out.format("%s : %s \n", ev.kind().name(), child);
				
				String fileName = child.toString();
				File file = new File(fileName);
				if (kind == ENTRY_CREATE) {
					try {
						if (Files.isDirectory(child, NOFOLLOW_LINKS)) {
                            register(child);
                        }
						
						/* Upload file to S3 */
						s3Client.putObject(new PutObjectRequest("bucket-for-share", fileName, file).withCannedAcl(CannedAccessControlList.PublicRead));
					} catch(IOException e) {
						return;
					}
				}
				
				/* Delete file in S3 bucket */
				if (kind == ENTRY_DELETE) {
					s3Client.deleteObject("bucket-for-share", fileName);
				}
				
				/* reset */
				boolean valid = key.reset();
				if (!valid) {
					keys.remove(key);
					if (keys.isEmpty()) {
						break;
					}
				}
			}
			
			System.out.println("Thread exits");
		}
	}
	
}

