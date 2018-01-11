//BRICK PROGRAM Spring 2015

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.BTConnection;
import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.RS485;
import lejos.nxt.comm.USB;
import lejos.nxt.comm.USBConnection;
import lejos.util.Timer;
import lejos.util.TimerListener;

public class MasterBrickNew implements TimerListener {

	static double seconds = 0;

	static double record;
		
	static Timer timer = new Timer(10, new MasterBrickNew());
	
	static boolean prevSensor = true;

	// Variables that should be customized before a trip.

	static final double LENGTH_BETWEEN_SENSORS = 29.76;

	static int start;

	static double scheduledTime;
	
	static double totalDistance  = 0;

	static double desiredSpeed;

	static double startingSpeed;

	static int[][] schedule = new int[6][3];//Sub trip array: 0 holds subtrip destination,
	// 1 holds action to perform at loader, 2 holds number
	// of blocks.
	
	static boolean turnout = true;
	
	static int loader = 1;// 1 unload, 2 load

	static int loader2 = 1;// 1 unload, 2 load
	
	static double compensation = 0; //makes the train behave when it has to visit loader
	
	static int numBlocks = 2; //Number of blocks for next load/unload.

	// Data streams to/from the pc

	static DataOutputStream toPC;

	static DataInputStream fromPC;
	
	static DataOutputStream toLoader;
	
	static DataInputStream fromLoader;
	
	static DataOutputStream toLoader2;
	
	static DataInputStream fromLoader2;
	
	public static void main(String[] args) throws Exception {
		
		//Fill in schedule array
		for(int i = 0; i < 6; i++){
			for(int j = 0; j < 3; j++){
				schedule[i][j] = 0;
			}
		}

		//Initialize global variables
		scheduledTime = 0;
		
		// Connect to slave brick
		LCD.drawString("Connecting Slave...", 0, 0);

		NXTConnection con = RS485.getConnector().connect("SLAVE",
				NXTConnection.RAW);
		Sound.buzz();

		LCD.clear();

		DataInputStream fromSlave = con.openDataInputStream();
		
		//Wait for Loader brick
		BTConnection BTLink = Bluetooth.connect(Bluetooth.getKnownDevice("GRAB1"));
		LCD.drawString("Connecting Loader...", 0, 0);
	
//		BTConnection BTLink2 = Bluetooth.connect(Bluetooth.getKnownDevice("GRAB2"));
//		LCD.drawString("Connecting Loader2...", 0, 1);
		
		toLoader = BTLink.openDataOutputStream();
		fromLoader = BTLink.openDataInputStream();
		
		Sound.buzz();
		
//		toLoader2 = BTLink2.openDataOutputStream();
//		fromLoader2 = BTLink2.openDataInputStream();
		
		Sound.buzz();
				
		LCD.clear();
		
		// Wait for PC connection
		USBConnection USBLink;

		LCD.drawString("Waiting on PC...", 0, 0);
		USBLink = USB.waitForConnection();
		toPC = USBLink.openDataOutputStream();
		fromPC = USBLink.openDataInputStream();

		LCD.clear();

		// Initialize sensors
		UltrasonicSensor one = new UltrasonicSensor(SensorPort.S1);
		UltrasonicSensor two = new UltrasonicSensor(SensorPort.S2);
		UltrasonicSensor three = new UltrasonicSensor(SensorPort.S3);

		Motor.A.setSpeed(600);
					
		// read in trip data from pc
		try {
			schedule[0][0] = fromPC.readInt();
			
			for (int x = 1; x < 6 && schedule[x-1][0] != -5; x++) {
				schedule[x][0] = fromPC.readInt();
				
				//Calculate total distance
				if(schedule[x][0] != -5)
					totalDistance += distance(schedule[x-1][0], schedule[x][0], x);
			}
			
			// Time for entire trip
			scheduledTime = fromPC.readInt();
			
		} catch (IOException ioe) {
			System.out.println("\nIO Exception reading from PC");
		}
		
		desiredSpeed = totalDistance/ (scheduledTime - compensation);
		
		start = schedule[0][0];

		// START TRIP___________________________________________________________
		timer.start();
		
		speedAdjuster(0);
		
		int ID = 0;
		int remoteID = 0;		
		
		for (int x = 0; x < schedule.length - 1 && schedule[x+1][0] != -5; x++) {

			//Start Route Calculation______________________
			
			//Set variables to current schedule data
			int destination = schedule[x + 1][0];
			
			//double s1 = 0;
			//double s2 = 0;

			if (destination == 6 && !turnout){
				turnout = true;
				Turnout(1);
			}
			
			if ((destination == 4 || destination == 5) && turnout) {
					turnout = false;
					Turnout(-1);
			}

			/*adjust turnouts (defaulted to true - the inner track)
			if (Math.abs(12 - s1) < Math.abs(12 - s2)) {
				turnout = false;
				Turnout(-1);
			} else {
				turnout = true;
			}
			*/			
			//End Route Calculation____________________________

			while (ID != destination) {
				if (one.getDistance() < 11 && ID != 1) {
					speedometer(1);
					ID = 1;
				}
				if (two.getDistance() < 11 && ID != 2) {
					if(loader != 0)
						Loader();
					else {
						speedometer(2);
					}
					ID = 2;
				}
				if (three.getDistance() < 11 && ID != 3) {
					speedometer(3);
					ID = 3;
				} 
				else {
					try {
						if (fromSlave.available() > 0) { 
							remoteID = fromSlave.readInt();
							fromSlave.close();
						}
						if (remoteID == 4 && ID != 4) {
							if(loader2 != 0) {
								Loader2();
							}
							else {
								speedometer(4);
							}
							remoteID = 0;
							ID = 4;
						}
						if (remoteID == 5 && ID != 5) {
							speedometer(5);
							remoteID = 0;
							ID = 5;
						}
						if (remoteID == 6 && ID != 6) {
							speedometer(6);
							remoteID = 0;
							ID = 6;
						}
					} catch (IOException ioe) {
						Sound.beepSequence();
						continue;
					}
				}
				// else
				// write dummy value to Loader
				toLoader.writeInt(0);
				toLoader.flush();
				
				// write dummy value to PC
				toPC.writeInt(0);
				toPC.flush();
			}// while
			
		}// outer loop
		timer.stop();
		
		speedAdjuster(desiredSpeed * 2.5);

		toPC.writeDouble(seconds);
		toPC.flush();

		if (!turnout) {
			turnout = true;
			Turnout(1);
		}
		// END TRIP________________________________________________________
		
		// Tell gripper it's done
		toLoader.writeInt(3);
		
		//close data streams
		toLoader.close();
		fromLoader.close();
		fromSlave.close();
		con.close();

	}// main

	public void timedOut() {
		seconds = seconds + 0.01;
	} // timedOut()

	public static void speedometer(int currentSensor) throws IOException,
			InterruptedException {
		
		toPC.writeInt(currentSensor);

		Sound.beep();
		
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
		} else if (currentSensor == 5) {
			currentSpeed = (LENGTH_BETWEEN_SENSORS - 15)/ timeInterval;
			totalDistance -= (LENGTH_BETWEEN_SENSORS - 15);
		}else {
			currentSpeed = (LENGTH_BETWEEN_SENSORS) / timeInterval;
			totalDistance -= LENGTH_BETWEEN_SENSORS;
		}
		
		if (remainingTime > 0) // train's got time left
			desiredSpeed = totalDistance / (remainingTime - compensation);
		else
			// train is late, i.e. negative time
			desiredSpeed = 12; // Speedy, but not too fast.

		// Print stuff
		LCD.drawString("ID: " + currentSensor, 0, 0);
		LCD.drawString("DS: " + desiredSpeed, 0, 1);
		LCD.drawString("CS: " + currentSpeed, 0, 2);
		LCD.drawString("TI: " + timeInterval, 0, 3);

		// Adjust speed
		speedAdjuster(currentSpeed);

		// Reset record (time taken)
		if (seconds > 0)
			record = seconds;

		// Write values to PC
		toPC.writeDouble(desiredSpeed);
		toPC.writeDouble(seconds);
		toPC.flush();

	}// speedometer

	// the method used to convert speed readings into degrees of turning.

	public static void speedAdjuster(double currentSpeed) {

		// Top speed is approximately 40 in/sec without cart
		// 28 with cart
//		if (desiredSpeed > 12) {
//			desiredSpeed = 12;
//		}

		// This is caused by compensation being greater than the timeInterval
		// resulting in a negative desired speed.
		if (desiredSpeed < 0) {
			desiredSpeed = 12;
		}
		
		if(currentSpeed > 1000){
			currentSpeed = 0;
		}
		
//		if (currentSpeed < 0) {
//			currentSpeed = 0;
//		}
		
		// number is how much to change the speed.
		// positive for faster, negative for slower
		double number = desiredSpeed - currentSpeed;

		//if (Math.abs(number) <= desiredSpeed) {
			Motor.A.rotate((int) ((number / 1.9) * 10));
		//}
	}// adjuster

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
				
				// compensation here is used in time calculations
				// this is the time spent to unload or load numBlocks
				// this needs to be rechecked and possibly reworked
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

	public static void Turnout(double direction) {

		// Positive direction sets turnout to true (inner), negative direction sets to false(outer)
		Motor.B.rotate((int) (Math.signum(direction) * 25));
		Motor.C.rotate((int) (Math.signum(direction) * 22));
	}// turnout

	
	public static void Loader() throws IOException{
		
		toLoader.writeInt(loader);
		toLoader.flush();
		
		Sound.beep();
		
		double currentSpeed = (LENGTH_BETWEEN_SENSORS) / (seconds - record);
		double prevSpeed = desiredSpeed;
		desiredSpeed = 3;
		record = seconds;
		
		speedAdjuster(currentSpeed);
	
		desiredSpeed = 0;
		
		int flag = fromLoader.readInt();
		
		toPC.writeInt(2);
		toPC.writeDouble(desiredSpeed);
		toPC.writeDouble(seconds);
		toPC.flush();
		
		while(flag != 1){
			flag = fromLoader.readInt();
		}
		
		speedAdjuster(5.25);
		
		LCD.drawString("Loading...", 0, 0);
		
		//Receive negligible int from loader as signal to resume.
		fromLoader.readInt();
		
		if(loader == 1)
			loader = 2;
		else
			loader = 0;
		
		compensation -= numBlocks * 7 + 5;
		totalDistance -= 15;
		
		desiredSpeed = prevSpeed;
		speedAdjuster(0);
		
		LCD.clear();
		
	}

	public static void Loader2() throws IOException{
		
		LCD.clear();
		LCD.drawString("ONE", 0, 0);
		toPC.writeInt(128);
		toPC.writeInt(loader2);
		toPC.flush();
		LCD.clear();
		LCD.drawString("TWO", 0, 0);

		Sound.beep();
		
		double currentSpeed = (LENGTH_BETWEEN_SENSORS) / (seconds - record);
		double prevSpeed = desiredSpeed;
		desiredSpeed = 3; //was 5
		record = seconds;
		
		
		speedAdjuster(currentSpeed);
	
		desiredSpeed = 0;
		LCD.clear();
		LCD.drawString("THREE", 0, 0);

		int flag = fromPC.readInt();
		LCD.clear();
		LCD.drawString("FOUR", 0, 0);

		toPC.writeInt(2);
		toPC.writeDouble(desiredSpeed);
		toPC.writeDouble(seconds);
		toPC.flush();
		
		LCD.clear();
		LCD.drawString("FIVE", 0, 0);

		
		while(flag != 1){
			flag = fromPC.readInt();
		}
		
		LCD.clear();
		LCD.drawString("SIX", 0, 0);

		
		speedAdjuster(5.15);
		
		LCD.drawString("Loading...", 0, 0);
		
		//Receive negligible int from loader as signal to resume.
		fromPC.readInt();
		
		LCD.clear();
		LCD.drawString("SEVEN", 0, 0);

		if(loader2 == 1)
			loader2 = 2;
		else
			loader2 = 0;
		
		compensation -= numBlocks * 7 + 5;
		totalDistance -= 15;
		
		desiredSpeed = prevSpeed;
		speedAdjuster(0);
		
		LCD.clear();
		
	}

}// class end