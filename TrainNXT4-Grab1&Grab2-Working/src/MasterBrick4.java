//BRICK PROGRAM Spring 2015, Fall 2017
//Cody Martin

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.LCD;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.USB;
import lejos.nxt.comm.USBConnection;

public class MasterBrick4{

	static int ADJUST_SPEED_FLAG = 256;
	
	static int TURN_OUT_FLAG = 128;
	
	// Data streams to/from the pc

	static DataOutputStream toPC;

	static DataInputStream fromPC;
		
	static int turnoutDegrees;
	
	public static void main(String[] args) throws Exception {
		
		
		// Wait for PC connection
		USBConnection USBLink;

		LCD.drawString("Waiting on PC...", 0, 0);
		USBLink = USB.waitForConnection();
		toPC = USBLink.openDataOutputStream();
		fromPC = USBLink.openDataInputStream();

		LCD.clear();

		Sound.beepSequenceUp();
		
		// Initialize sensors
		UltrasonicSensor one = new UltrasonicSensor(SensorPort.S1);
		UltrasonicSensor two = new UltrasonicSensor(SensorPort.S2);
		UltrasonicSensor three = new UltrasonicSensor(SensorPort.S3);

		Motor.A.setSpeed(600);

		int stop = 0;
		int flag = 0;
		int ID = 0;
		int writes = 0;
		int reads = 0;
		
//		boolean resetCon = false;
		
		while( stop == 0 ) {
//			if(resetCon) {
//				USBLink = USB.waitForConnection();
//				toPC = USBLink.openDataOutputStream();
//				fromPC = USBLink.openDataInputStream();
//			}

			LCD.clear();
			LCD.drawString("One -\n Reads: " + reads + "\n Writes : " + writes, 0, 0);
			try {
				if (one.getDistance() < 11 && ID != 1) {
					LCD.clear();
					LCD.drawString("Two -\n Reads: " + reads + "\n Writes :" + writes, 0, 0);
					toPC.writeInt(1);
					toPC.flush();
					writes++;
//					Sound.beep();
					ID = 1;
				}
				if (two.getDistance() < 11 && ID != 2) {
					LCD.clear();
					LCD.drawString("Three -\n Reads: " + reads + "\n Writes :" + writes, 0, 0);
					toPC.writeInt(2);
					toPC.flush();
//					Sound.beep();
					writes++;
					ID = 2;
				}
				if (three.getDistance() < 11 && ID != 3) {
					LCD.clear();
					LCD.drawString("Four -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
					toPC.writeInt(3);
					toPC.flush();
//					Sound.beep();
					writes++;
					ID = 3;
				}
				if (fromPC.available() >= 0) {
					LCD.clear();
					LCD.drawString("Five -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
//					Sound.beepSequenceUp();
					flag = fromPC.readInt();
					reads++;
					if(flag == ADJUST_SPEED_FLAG) {
						LCD.clear();
						LCD.drawString("Six -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
						
						Motor.A.rotate(fromPC.readInt());
						reads++;
						LCD.clear();
						LCD.drawString("Seven -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
						
//						Sound.beepSequenceUp();
					}
					else if(flag == TURN_OUT_FLAG) {
						LCD.clear();
						LCD.drawString("Eight -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
						
						turnoutDegrees = fromPC.readInt();
						reads++;
						Motor.B.rotate(turnoutDegrees);
						Motor.C.rotate(turnoutDegrees);
//						Sound.beepSequenceUp();
						LCD.clear();
						LCD.drawString("Nine -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
						
					}
					else if(flag == -1) {
						LCD.clear();
						LCD.drawString("Ten -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
						
						stop = flag;
//						Sound.beepSequence();
					}
				}
				
				LCD.clear();
				LCD.drawString("Eleven -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
				
				toPC.writeInt(0);
				toPC.flush();
				
				writes++;
				
				LCD.clear();
				LCD.drawString("Twelve -\n Reads: " + reads + "\n Writes: " + writes, 0, 0);
				
//				toPC.close();
//				fromPC.close();

//				fromPC = USBLink.openDataInputStream();
//				toPC = USBLink.openDataOutputStream();
//				USBLink.close();
//				resetCon = true;
				
			}
			catch(IOException ioe) {
				Sound.buzz();
				System.exit(1);
			}
		}// loop
		

		USBLink.close();
		
	}//main end
}// class end