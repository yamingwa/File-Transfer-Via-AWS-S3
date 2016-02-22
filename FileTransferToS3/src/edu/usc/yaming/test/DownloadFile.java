package edu.usc.yaming.test;

import java.io.*;
import java.util.Scanner;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;

public class DownloadFile {

	public static void main(String[] args) {
		AmazonS3 s3 = new AmazonS3Client(new ProfileCredentialsProvider());
		String bucketName = "bucket-for-share";
		String s3Path = "/Users/Daniel/Desktop/share";
		String localPath = "/Users/Daniel/Desktop/share1";
		
		
		usage();
		
		boolean quit = false;
		Scanner scanner = new Scanner(System.in);
		String command = scanner.nextLine();
		while (!quit) {
			
			String[] temp = command.split(" ");
		
			/* List Files */
			if (temp[0].compareToIgnoreCase("ls") == 0) {
				ObjectListing obList = s3.listObjects(new ListObjectsRequest()
										 .withBucketName(bucketName)
										 .withPrefix(s3Path));
			
				for (S3ObjectSummary objectSummary : obList.getObjectSummaries()) {
					System.out.println(objectSummary.getKey() + "  " +
									   "(size = " + objectSummary.getSize() + ")");
				}
			}
		
			/* Download file */
			if (temp[0].compareToIgnoreCase("get") == 0) {
				String fileName = s3Path + "/" + temp[1];
			
				try {
					File file = new File(localPath + "/" + temp[1]);
					s3.getObject(new GetObjectRequest(bucketName, fileName), file);
				
				} catch(Exception e) {
					System.out.println("File not exits!");
				}
				System.out.println("Downloading finished");
			}
			
			command = scanner.nextLine();
			if (command.compareToIgnoreCase("exit") == 0) {
				quit = true;
			}

		}
	}
	
	public static void usage() {
		System.out.println("Command List: 1. ls; 2: get [filename]");
	}
}
