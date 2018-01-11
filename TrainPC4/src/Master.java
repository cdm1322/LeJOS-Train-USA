//PC program Spring 2015 & Fall 2017
//Cody Martin

import java.io.*;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

import lejos.pc.comm.NXTConnector;

public class Master {
	
	static double seconds = 0;

	static int sensorTrainSlave = 0;
	
	static int sensorCenterBrick = 0;
	
	static int TURN_OUT_FLAG = 128;
	
	static int ADJUST_SPEED_FLAG = 256;
	
	static double record;

	static boolean prevSensor = true;
		
	static Timer timer = new Timer();
	
	static int[][] schedule = new int[6][3];//Sub trip array: 0 holds subtrip destination,
	// 1 holds action to perform at loader, 2 holds number
	// of blocks.

	static double totalDistance  = 0;

	static final double LENGTH_BETWEEN_SENSORS = 29.76;

	static double scheduledTime;

	static double desiredSpeed;

	static double startingSpeed;

	static boolean turnout = true;
	
	static int loader = 1;// 1 unload, 2 load

	static int loader2 = 1;// 1 unload, 2 load
	
	static double compensation = 0; //makes the train behave when it has to visit loader
	
	static int numBlocks = 2; //Number of blocks for next load/unload.

	static int start;

	static int writesMaster = 0;
	
	// Output Streams
	static OutputStream outGrabber1;
	static OutputStream outGrabber2;
	static OutputStream outTrainSlave;
	static OutputStream outCenterBrick;
	
	// Input Streams
	static InputStream inGrabber1;
	static InputStream inGrabber2;
	static InputStream inTrainSlave;
	static InputStream inCenterBrick;

	static NXTConnector linkGrabber1 = new NXTConnector();
	static NXTConnector linkGrabber2 = new NXTConnector();
	static NXTConnector linkTrainSlave = new NXTConnector();
	static NXTConnector linkCenterBrick = new NXTConnector();
	
	static DataOutputStream outDataGrabber1;
	static DataInputStream inDataGrabber1;

	static DataOutputStream outDataGrabber2;
	static DataInputStream inDataGrabber2;
	
	static DataOutputStream outDataTrainSlave;
	static DataInputStream inDataTrainSlave;
	
	static DataOutputStream outDataCenterBrick;
	static DataInputStream inDataCenterBrick;
	
		public static void main(String[] args) throws IOException, InterruptedException {

		// initialize variables
	
		// User input handler
		Scanner kb = new Scanner(System.in);
		
		// arrays to store schedule data for display purposes
		int[] stops = new int[5];

		// try to connect to NXT bricks

		// Check if all connections were made
		if (!linkGrabber1.connectTo("usb://GRAB1")) {
			System.out.println("\nNo GRAB1 found using USB");
		}
		if (!linkGrabber2.connectTo("usb://GRAB2")) {
			System.out.println("\nNo GRAB2 found using USB");
		}
		if (!linkTrainSlave.connectTo("usb://SLAVE")) {
			System.out.println("\nNo SLAVE found using USB");
		}
		if (!linkCenterBrick.connectTo("usb://CENTER")) {
			System.out.println("\nNo CENTER found using USB");
		}
		
		// get streams from connections
		
		// Grabber 1
		outGrabber1 = linkGrabber1.getOutputStream();
		inGrabber1 = linkGrabber1.getInputStream();
		System.out.println("\nGRAB1 is Connected");
		outDataGrabber1 = new DataOutputStream(outGrabber1);
		inDataGrabber1 = new DataInputStream(inGrabber1);

		// Grabber 2
		outGrabber2 = linkGrabber2.getOutputStream();
		inGrabber2 = linkGrabber2.getInputStream();
		System.out.println("\nGRAB2 is Connected");
		outDataGrabber2 = new DataOutputStream(outGrabber2);
		inDataGrabber2 = new DataInputStream(inGrabber2);
		
		// Train Slave
		outTrainSlave = linkTrainSlave.getOutputStream();
		inTrainSlave = linkTrainSlave.getInputStream();
		System.out.println("\nSLAVE is Connected");
		outDataTrainSlave = new DataOutputStream(outTrainSlave);
		inDataTrainSlave = new DataInputStream(inTrainSlave);

//		// Center Brick
		outCenterBrick = linkCenterBrick.getOutputStream();
		inCenterBrick = linkCenterBrick.getInputStream();
		System.out.println("\nCENTER is Connected");
		outDataCenterBrick = new DataOutputStream(outCenterBrick);
		inDataCenterBrick = new DataInputStream(inCenterBrick);

		// enter start & send to system
		System.out.println("Enter start: ");
		start = kb.nextInt();

		schedule[0][0] = start;
		
		int dest = 0;
		int counter = 0; // makes sure schedule does not exceed limit
		int j = 1;
		
		do {
			// enter destination
			System.out.println("Enter destination or -5 to finish: ");
			dest = kb.nextInt();

			// Fill Data Arrays
			schedule[j][0] = dest;
			
			// Calculate Distance
			if(schedule[j][0] != -5 && j != 0) {
				totalDistance += distance(schedule[j-1][0], schedule[j][0], j);
			}
			
			stops[counter] = dest;
			counter++;
			j++;
			
		} while (dest != -5 && counter < 5);
		
		// enter time
		int time;
		System.out.println("Enter time schedule: ");
		time = kb.nextInt();
		
		scheduledTime = time;
		
		// display full schedule
		System.out.println("Schedule:\nFrom " + start + " to " + stops[0]);
		
		for (int i = 1; i < counter && stops[i] != -5; i++) {
			System.out.println("From " + stops[i - 1] + " to " + stops[i]);
		}
		
		System.out.println("Scheduled Time: " + time);
		
		desiredSpeed = totalDistance/ (scheduledTime - compensation);
		
//		System.out.println("Test Spot 1");
		
		start = schedule[0][0];

//		System.out.println("Test spot 2");
		
		//Start Trip
		TimerTask task = new TimerTask() {

			@Override
			public void run() {
				timedOut();
			}
			
		};
		
		timer.schedule(task,  0, 10);
		
		speedAdjuster(0);
		
		int ID = 0;
		
//		System.out.println("Test spot 3");
		
//		boolean resetCon = false;
		
		for (int x = 0; x < schedule.length - 1 && schedule[x+1][0] != -5; x++) {

			//Start Route Calculation______________________
			
			//Set variables to current schedule data
			int destination = schedule[x + 1][0];
			System.out.println("destination: " + destination);
			
			if (destination == 6 && !turnout){
				System.out.println("One");
				turnout = true;
				try {
					outDataCenterBrick.writeInt(TURN_OUT_FLAG);
					outDataCenterBrick.flush();
					outDataCenterBrick.writeInt((int)(Math.signum(1) * 25));
					outDataCenterBrick.flush();
					writesMaster += 2;
				} catch (Exception ioe) {
					System.out.println("IOException Occurred");
				}
			}
			
			if (destination == 4 || destination == 5 && turnout) {
				System.out.println("Two");
				turnout = false;
				try {
					outDataCenterBrick.writeInt(TURN_OUT_FLAG);
					outDataCenterBrick.flush();
					outDataCenterBrick.writeInt((int)(Math.signum(-1) * 25));
					outDataCenterBrick.flush();
					writesMaster += 2;
				} catch (Exception ioe) {
					System.out.println("IOException Occurred");
					continue;
				}
			}

			int countSlave = 0;
			int countMaster = 0;
			
			while (ID != destination) {
				System.out.println("Three");
				// Center Brick
//				if(resetCon) {
//					linkCenterBrick.connectTo("usb://CENTER");
//					outCenterBrick = linkCenterBrick.getOutputStream();
//					inCenterBrick = linkCenterBrick.getInputStream();
//					System.out.println("\nCENTER is Connected");
//					outDataCenterBrick = new DataOutputStream(outCenterBrick);
//					inDataCenterBrick = new DataInputStream(inCenterBrick);
//				}
				try {
					if(inDataTrainSlave.available() >= 0) {
						System.out.println("Four");
						countSlave++;
						sensorTrainSlave = inDataTrainSlave.readInt();
						System.out.println("Train Slave Sensor: " + sensorTrainSlave);
						System.out.println("Slave Reads: " + countSlave);
					}
					if(inDataCenterBrick.available() >= 0) {
						System.out.println("Five");
						countMaster++;
						sensorCenterBrick = inDataCenterBrick.readInt();
						System.out.println("Center Brick Sensor: " + sensorCenterBrick);
						System.out.println("Master Reads: " + countMaster);
						
					}
//					System.out.println("Number of Reads: " + count);
					
					if (sensorCenterBrick == 1 && ID != 1) {
						speedometer(1, outDataCenterBrick);
						ID = 1;
					}
					if (sensorCenterBrick == 2 && ID != 2) {
						if(loader != 0)
							Loader();
						else {
							speedometer(2, outDataCenterBrick);
						}
						ID = 2;
					}
					if (sensorCenterBrick == 3 && ID != 3) {
						speedometer(3, outDataCenterBrick);
						ID = 3;
					}
					
					if (sensorTrainSlave == 4 && ID != 4) {
						if(loader2 != 0) {
							Loader2();
						}
						else {
							speedometer(4, outDataCenterBrick);
						}
						ID = 4;
					}
					if (sensorTrainSlave == 5 && ID != 5) {
						speedometer(5, outDataCenterBrick);
						ID = 5;
					}
					if (sensorTrainSlave == 6 && ID != 6) {
						speedometer(6, outDataCenterBrick);
						ID = 6;
					}
				

					// write dummy value to Grabber 1
					outDataGrabber1.writeInt(0);
					outDataGrabber1.flush();
					
					// write dummy value to Grabber 2
					outDataGrabber2.writeInt(0);
					outDataGrabber2.flush();
					
					// write dummy value to Slave Brick
					outDataTrainSlave.writeInt(0);
					outDataTrainSlave.flush();
					
					System.out.println("Seven");

					System.out.println("Writes to master: " + writesMaster);
					System.out.println("# of bytes written: " + outDataCenterBrick.size());

					// write dummy value to Master Brick
					outDataCenterBrick.writeInt(0);
					outDataCenterBrick.flush();

					System.out.println("Six");

					writesMaster++;

//					inDataCenterBrick.close();
//					outDataCenterBrick.close();
//					inCenterBrick.close();
//					outCenterBrick.close();
//					
//					outCenterBrick = linkCenterBrick.getOutputStream();
//					inCenterBrick = linkCenterBrick.getInputStream();
//					outDataCenterBrick = new DataOutputStream(outCenterBrick);
//					inDataCenterBrick = new DataInputStream(inCenterBrick);
//					linkCenterBrick.close();
//					resetCon = true;
					
				}catch (Exception ioe) {
					System.out.println("IOException Occurred");	
					continue;
				}
			}// while
		}// outer loop
		timer.cancel();
			
		speedAdjuster(desiredSpeed * 2.5);
	
		if (!turnout) {
			turnout = true;
//			Turnout(1);
			outDataCenterBrick.writeInt(TURN_OUT_FLAG);
			outDataCenterBrick.flush();
			outDataCenterBrick.writeInt((int)(Math.signum(1) * 25));
			outDataCenterBrick.flush();
			writesMaster += 2;
		}
		// END TRIP________________________________________________________
			
		try {
			
		// Tell Grabber1 it's done
		outDataGrabber1.writeInt(3);
		outDataGrabber1.flush();
		
		// Tell Grabber2 it's done
		outDataGrabber2.writeInt(3);
		outDataGrabber2.flush();
		
		// Tell Center Brick it's done
		outDataCenterBrick.writeInt(-1);
		outDataCenterBrick.flush();
		writesMaster++;
		
		// Tell Train Slave it's done
		outDataTrainSlave.writeInt(-1);
		outDataTrainSlave.flush();

		} catch (Exception ioe) {
			System.out.println("IOException Occurred.");
		}
		
		//For each destination in the schedule
//		for (int i = 0; i < counter && stops[i] != -5; i++) {
			// receive data from bricks until train reaches destination
//			try {
//				int locationTrainSlave = 0; //Initialize to dummy value
//				int locationCenterBrick = 0; //Initialize to dummy value
				
				//While the train does not hit the current destination
//				while (locationTrainSlave != stops[i] || locationCenterBrick != stops[i]) {
//					locationTrainSlave = inDataTrainSlave.readInt();	//will be 0 until the train triggers a sensor
//					locationCenterBrick = inDataCenterBrick.readInt();	//will be 0 until the train triggers a sensor

					//Only enter if train triggers sensor other than destination
//					if (locationTrainSlave != 0 || locationCenterBrick != 0) {
//						if(locationTrainSlave != 0) { // sensor triggered from TrainSlave
//							double speed = inDataTrainSlave.readDouble();
//							double sec = inDataTrainSlave.readDouble();
//							System.out.println("\nCurrent Sensor: " + locationTrainSlave);
//							System.out.println("Desired Speed: " + speed
//									+ " in/sec");
//							System.out.println("Time taken: " + sec + " seconds");

//						}
//						else { // sensor triggered from CenterBrick
//							double speed = inDataCenterBrick.readDouble();
//							double sec = inDataCenterBrick.readDouble();
//							System.out.println("\nCurrent Sensor: " + locationCenterBrick);
//							System.out.println("Desired Speed: " + speed
//									+ " in/sec");
//							System.out.println("Time taken: " + sec + " seconds");

//						}
						
						
//						if(locationTrainSlave == stops[i] || locationCenterBrick == stops[i]) {
//							break;
//						}
//					}//end if
					
//				}// end loop
											
//			} catch (IOException ioe) {
//				System.out.println("\nIO Exception reading from brick");
//			}
//		}// outer loop
//		read in final data
	
		try{
			
			// Grabber 1
			inDataGrabber1.close();
			outDataGrabber1.close();
			linkGrabber1.close();
			
			// Grabber 2
			inDataGrabber2.close();
			outDataGrabber2.close();
			linkGrabber2.close();
			
			// Train Slave
			inDataTrainSlave.close();
			outDataTrainSlave.close();
			linkTrainSlave.close();
			
			// Center Brick
//			inDataCenterBrick.close();
//			outDataCenterBrick.close();
			linkCenterBrick.close();
			
			
		} catch (IOException ioe) {
			System.out.println("\nIO Exception closing streams");
		}
		System.out.println("\nClosed data streams");

		kb.close();
		System.exit(0);
		
	}// End main

//	public static void Turnout(double direction) {

		// Positive direction sets turnout to true (inner), negative direction sets to false(outer)

		// Send these values to TrainSlave //
		
//		Motor.B.rotate((int) (Math.signum(direction) * 25));
//		Motor.C.rotate((int) (Math.signum(direction) * 25));
//	}// turnout

	public static void speedometer(int currentSensor, DataOutputStream stream) throws IOException,
		InterruptedException {

		
			
			// Adjust prevSensor
			if(currentSensor == 6){
				prevSensor = true;
			}
			else if (currentSensor == 5){
				prevSensor = false;
			}
			
			// Initialize local variables
			double remainingTime = scheduledTime - seconds;
			double currentSpeed = 0;
			double timeInterval = seconds - record;
			
			// Calculate current speed & adjust total distance
			if(currentSensor == start){
				currentSpeed = 0;
				start = 0;
			} else if (currentSensor == 1 && prevSensor) {// adjust for non-standard distances
				currentSpeed = 44 / timeInterval;
				totalDistance -= 44;
			} else if (currentSensor == 3){
				currentSpeed = (LENGTH_BETWEEN_SENSORS - 15)/ timeInterval;
				totalDistance -= (LENGTH_BETWEEN_SENSORS - 15);
			}else {
				currentSpeed = (LENGTH_BETWEEN_SENSORS) / timeInterval;
				totalDistance -= LENGTH_BETWEEN_SENSORS;
			}
			
			if (remainingTime > 0) // train's got time left
			{
				desiredSpeed = Math.abs(totalDistance / (remainingTime - compensation));
//				if (desiredSpeed < 0) {
//					
//					// Not entirely sure why this is happening, quick temporary fix. 
//					// To be investigated more though
//					System.out.println("Remaining Time: " + remainingTime);
//					System.out.println("Compensation: " + compensation);
//					desiredSpeed = 6;
//				}
			} else
				// train is late, i.e. negative time
				desiredSpeed = 12; // Speedy, but not too fast.
			
			// Print stuff
			System.out.println("ID: " + currentSensor);
			System.out.println("DS: " + desiredSpeed);
			System.out.println("CS: " + currentSpeed);
			System.out.println("TI: " + timeInterval);
			
			// Adjust speed
			stream.writeInt(ADJUST_SPEED_FLAG);
			stream.flush();
			stream.writeInt(speedAdjuster(currentSpeed));
			stream.flush();
			writesMaster += 2;
			
			// Reset record (time taken)
			if (seconds > 0)
				record = seconds;
			
			// Write values to PC
			System.out.println("\nCurrent Sensor: " + currentSensor);
			System.out.println("Desired Speed: " + desiredSpeed                   
					+ " in/sec");                                          
			System.out.println("Time taken: " + seconds + " seconds");         
		
	}// speedometer

	
	public static double distance(int id, int tripEnd, int subtrip) {
		double distance = 0;

		while (id != tripEnd) {
			if (id == 6) {// If at 6, add uncommon distance and loop to 1
				distance = distance + 44;
				id = 1;
			} else if (id == 5) {// If at 5, add normal distance and loop to 1
				distance = distance + LENGTH_BETWEEN_SENSORS;
				id = 1;
			} else if (id == 3) {// If at three determine turnout direction
				if (tripEnd == 4 || tripEnd == 5) {// Turnout to inner track: add distance to 6
					distance = distance + LENGTH_BETWEEN_SENSORS;
					id = 4;
				} else {// Turnout to outer track: add distance to 4
					distance = distance + LENGTH_BETWEEN_SENSORS;
					id = 6;
				}
			} else if (id == 2){// Identifies if the train will pass the loader
				schedule[subtrip][1] = 1;
				schedule[subtrip][2] = 2;
				compensation += numBlocks * 7 + 5;
				distance = distance + LENGTH_BETWEEN_SENSORS;
				id++;
			} else if (id == 4) {
				schedule[subtrip][1] = 1;
				schedule[subtrip][2] = 2;
				compensation += numBlocks * 7 + 5;
				distance = distance + LENGTH_BETWEEN_SENSORS;
				id++;
			} else {// Default: add normal distance and go to next sensor
				distance = distance + LENGTH_BETWEEN_SENSORS;
				id++;
			}
		}// loop

		return distance;
	}// distance

	public static void timedOut() {
		seconds = seconds + 0.01;
	} // timedOut()
	
	public static int speedAdjuster(double currentSpeed) {

		// Top speed is approximately 40 in/sec without cart
		// 28 with cart
		if (desiredSpeed > 12) {
			desiredSpeed = 12;
		}
		
		if(currentSpeed > 1000){
			currentSpeed = 0;
		}
		
		double number = desiredSpeed - currentSpeed;
		
		return ((int)((number / 1.9) * 10));
		//if (Math.abs(number) <= desiredSpeed) {
		
		//!!Send this to outDataCenterBrick!!//
		
//			Motor.A.rotate((int) ((number / 1.9) * 10));
		//}
	}// adjuster

	public static void Loader() throws IOException{
		
		outDataGrabber1.writeInt(loader);
		outDataGrabber1.flush();
		
		double currentSpeed = (LENGTH_BETWEEN_SENSORS) / (seconds - record);
		double prevSpeed = desiredSpeed;
		desiredSpeed = 5;
		record = seconds;
		
		outDataCenterBrick.writeInt(ADJUST_SPEED_FLAG);
		outDataCenterBrick.flush();
		outDataCenterBrick.writeInt(speedAdjuster(currentSpeed));
		outDataCenterBrick.flush();
		writesMaster += 2;
		
		desiredSpeed = 0;
		
		int flag = inDataGrabber1.readInt();
		System.out.println("Loader Test 1");
		
		
		// Write values to PC
		System.out.println("\nCurrent Sensor: " + 2);
		System.out.println("Desired Speed: " + desiredSpeed + " in/sec");                                          
		System.out.println("Time taken: " + seconds + " seconds");         
		
		while(flag != 1){
			flag = inDataGrabber1.readInt();
		}
		System.out.println("Loader Test 2");
		
		outDataCenterBrick.writeInt(ADJUST_SPEED_FLAG);
		outDataCenterBrick.flush();
		outDataCenterBrick.writeInt(speedAdjuster(5.5));
		outDataCenterBrick.flush();
		writesMaster += 2;
		
		System.out.println("Loading...");
		
		//Receive negligible int from loader as signal to resume.
		inDataGrabber1.readInt();
		System.out.println("Loader Test 3");
		if(loader == 1)
			loader = 2;
		else
			loader = 0;
		
		compensation -= numBlocks * 7 + 5;
		totalDistance -= 15;
		
		desiredSpeed = prevSpeed;
		
		outDataCenterBrick.writeInt(ADJUST_SPEED_FLAG);
		outDataCenterBrick.flush();
		outDataCenterBrick.writeInt(speedAdjuster(0));
		outDataCenterBrick.flush();
		writesMaster += 2;
		
		System.out.println("Loader Fin");
	}

	public static void Loader2() throws IOException{

		
		outDataGrabber2.writeInt(loader2);
		outDataGrabber2.flush();

		double currentSpeed = (LENGTH_BETWEEN_SENSORS) / (seconds - record);
		double prevSpeed = desiredSpeed;
		desiredSpeed = 1.5; //was 5
		record = seconds;
		
		
		outDataCenterBrick.writeInt(ADJUST_SPEED_FLAG);
		outDataCenterBrick.flush();
		outDataCenterBrick.writeInt(speedAdjuster(currentSpeed));
		outDataCenterBrick.flush();
		writesMaster += 2;
		
		desiredSpeed = 0;
		
		int flag = inDataGrabber2.readInt();
		
		// Write values to PC
		System.out.println("\nCurrent Sensor: " + 4);
		System.out.println("Desired Speed: " + desiredSpeed + " in/sec");                                          
		System.out.println("Time taken: " + seconds + " seconds");         
		
		
		while(flag != 1){
			flag = inDataGrabber2.readInt();
		}
		
		outDataCenterBrick.writeInt(ADJUST_SPEED_FLAG);
		outDataCenterBrick.flush();
		outDataCenterBrick.writeInt(speedAdjuster(5.5));
		outDataCenterBrick.flush();
		writesMaster += 2;
		
		System.out.println("Loading...");
		
		//Receive negligible int from loader as signal to resume.
		inDataGrabber2.readInt();
		
		
		if(loader2 == 1)
			loader2 = 2;
		else
			loader2 = 0;
		
		compensation -= numBlocks * 7 + 5;
		totalDistance -= 15;
		
		desiredSpeed = prevSpeed;
		
		outDataCenterBrick.writeInt(ADJUST_SPEED_FLAG);
		outDataCenterBrick.flush();
		outDataCenterBrick.writeInt(speedAdjuster(0));
		outDataCenterBrick.flush();
		writesMaster += 2;
	}
}//Master Class