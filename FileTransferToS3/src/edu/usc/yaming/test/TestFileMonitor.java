package edu.usc.yaming.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import edu.usc.yaming.watcher.FileWatcher;

public class TestFileMonitor {
	private static final Path dir = Paths.get("/Users/Daniel/Desktop/share");
	private static FileWatcher fileWatcher;
	public static void main(String[] args) {
		try {
			fileWatcher = new FileWatcher(dir);
		} catch(Exception e) {
			System.out.println("Error while creating watcher!!");
			System.exit(-1);
		}
		
		fileWatcher.processEvents();

	}

}
