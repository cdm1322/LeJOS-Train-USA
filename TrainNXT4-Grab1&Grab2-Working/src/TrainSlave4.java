import java.io.DataInputStream;
import java.io.DataOutputStream;

import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.USB;
import lejos.nxt.comm.USBConnection;

public class TrainSlave4{

	public static void main(String[] args){
		UltrasonicSensor four = new UltrasonicSensor(SensorPort.S1);
		UltrasonicSensor five = new UltrasonicSensor(SensorPort.S2);
		UltrasonicSensor six = new UltrasonicSensor(SensorPort.S3);
		
		int keepGoing = 0;
		
		

		LCD.drawString("Waiting...", 0, 2);

		try{
				
			USBConnection con = USB.waitForConnection();
//			BTConnection link;
			
//			link = Bluetooth.waitForConnection();
//			DataOutputStream dos = link.openDataOutputStream();
			
			LCD.drawString("Connected", 0, 2);
			
			DataOutputStream dos = con.openDataOutputStream();
			DataInputStream ios = con.openDataInputStream();
			
			int ID = 0;

			while (keepGoing == 0){
			
				//LCD.clear();
				//LCD.drawString("Continue...", 0, 4);
				
				

				if (four.getDistance() < 11 && ID != 4) {
//					Sound.beep();
					LCD.drawString("Writing", 0, 4);
					dos.writeInt(4);	
					dos.flush();
					LCD.drawInt(4, 0, 3);
					ID = 4;
				}
				if (five.getDistance() < 11 && ID != 5) {
//					Sound.beep();
					LCD.drawString("Writing", 0, 4);
					dos.writeInt(5);
					dos.flush();
					LCD.drawInt(5, 0, 3);
					ID = 5;
				}
				if (six.getDistance() < 11 && ID != 6) {
//					Sound.beep();
					LCD.drawString("Writing", 0, 4);
					dos.writeInt(6);
					dos.flush();
					LCD.drawInt(6, 0, 3);
					ID = 6;
				}
				
				LCD.drawInt(0, 0, 3);
				LCD.drawString("Writing 0's", 0, 4);
				dos.writeInt(0);
				dos.flush();
				
				if(ios.available() >= 0) {
					keepGoing = ios.readInt();
				}
				
			}//loop

			LCD.clear();
			
			dos.close();
			con.close();
			
		} catch (Exception e){
			Sound.buzz();
			LCD.drawString("Write Error", 0, 5);
			System.exit(1);
		}//catch
		
	}//main
	
}//class