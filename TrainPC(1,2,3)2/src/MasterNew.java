//PC program Spring 2015 & Fall 2017

import java.io.*;
import java.util.Scanner;

import lejos.pc.comm.NXTConnector;

public class MasterNew {

	public static void main(String[] args) throws IOException {
		// initialize variables
		OutputStream out;
		InputStream in;
		
		OutputStream outLoader;
		InputStream inLoader;
		
		Scanner kb = new Scanner(System.in);
		
		int loader2 = 128;
		int loaderFlag = 0;
		// arrays to store schedule data for display purposes
		int[] stops = new int[5];

		// try to connect to brick
		NXTConnector centerLink = new NXTConnector();
		NXTConnector loaderLink = new NXTConnector();
		
		if (!centerLink.connectTo("usb://CENTER")) {
			System.out.println("\nNo NXT CENTER found using USB");
		}
		if (!loaderLink.connectTo("usb://GRAB2")) {
			System.out.println("\nNo NXT GRAB2 found using USB");
		}
		
		// get streams for center
		out = centerLink.getOutputStream();
		in = centerLink.getInputStream();
		System.out.println("\nNXT is Connected");

		// get streams for loader
		outLoader = loaderLink.getOutputStream();
		inLoader = loaderLink.getInputStream();
		
		DataOutputStream outData = new DataOutputStream(out);
		DataInputStream inData = new DataInputStream(in);

		DataOutputStream outLoaderData = new DataOutputStream(outLoader);
		DataInputStream inLoaderData = new DataInputStream(inLoader);
		
		// enter start & send to master brick
		System.out.println("Enter start: ");
		int start = kb.nextInt();
		outData.writeInt(start);
		
		int dest = 0;
		int counter = 0; // makes sure schedule does not exceed limit
		
		do {
			// enter destination
			System.out.println("Enter destination or -5 to finish: ");
			dest = kb.nextInt();
			// send trip data to brick
			try {
				outData.writeInt(dest);
				outData.flush();
			} catch (IOException ioe) {
				System.out.println("\nIO Exception writing to brick");
			}
			
			// fill data arrays
			stops[counter] = dest;
			
			counter++;
		} while (dest != -5 && counter < 5);
		
		// enter time
		int time;
		System.out.println("Enter time schedule: ");
		time = kb.nextInt();
		
		outData.writeInt(time);
		outData.flush();				
		
		// display full schedule
		System.out.println("Schedule:\nFrom " + start + " to " + stops[0]);
		
		for (int i = 1; i < counter && stops[i] != -5; i++) {
			System.out.println("From " + stops[i - 1] + " to " + stops[i]);
		}
		
		System.out.println("Scheduled Time: " + time);
		
		//For each destination in the schedule
		for (int i = 0; i < counter && stops[i] != -5; i++) {
			// receive data from brick till train reaches
			// destination
			try {
				int location = 0;//Initialize to dummy value
				
				//While the train does not hit the current destination
				while (location != stops[i]) {
					location = inData.readInt();//will be 0 till the train triggers a sensor
					
					//Only enter if train triggers sensor other than destination
					if (location != 0 && location != loader2) {
						double speed = inData.readDouble();
						double sec = inData.readDouble();
						
						System.out.println("\nCurrent Sensor: " + location);
						System.out.println("Desired Speed: " + speed
								+ " in/sec");
						System.out.println("Time taken: " + sec + " seconds");
						
						if(location == stops[i])
							break;
						
					}//end if

					if(location == loader2) {
						// do stuff
						System.out.println("loader script started");
						loaderFlag = inData.readInt();
						outLoaderData.writeInt(loaderFlag);
						outLoaderData.flush();
						
						System.out.println("One");
						int flag = inLoaderData.readInt();
						outData.writeInt(flag);
						outData.flush();
						System.out.println("Two");
						location = inData.readInt();
						double speed = inData.readDouble();
						double sec = inData.readDouble();
						System.out.println("\nCurrent Sensor: " + location);
						System.out.println("Desired Speed: " + speed
								+ " in/sec");
						System.out.println("Time taken: " + sec + " seconds");
						
						while(flag!=1) {
							flag = inLoaderData.readInt();
							outData.writeInt(flag);
							outData.flush();
						}
						
						System.out.println("Three");
						
						// Receive and send negligible int from loader as signal to resume
						outData.writeInt(inLoaderData.readInt());
						outData.flush();
						
						System.out.println("Four");
					}

					
				}// end loop
											
			} catch (IOException ioe) {
				System.out.println("\nIO Exception reading from brick");
			}
		}// outer loop
		//read in final data
	
		outLoaderData.writeInt(3);
		outLoaderData.flush();
		
		try{
			
			inData.close();
			outData.close();
			centerLink.close();
			
			inLoaderData.close();
			outLoaderData.close();
			loaderLink.close();
			
		} catch (IOException ioe) {
			System.out.println("\nIO Exception closing streams");
		}
		System.out.println("\nClosed data streams");

		kb.close();
		System.exit(0);
	}// End main

}
