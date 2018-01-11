import java.io.DataOutputStream;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
//import lejos.nxt.comm.BTConnection;
//import lejos.nxt.comm.Bluetooth;
import lejos.nxt.comm.NXTConnection;
import lejos.nxt.comm.RS485;

public class TrainSlave{

	public static void main(String[] args){
		UltrasonicSensor four = new UltrasonicSensor(SensorPort.S1);
		UltrasonicSensor five = new UltrasonicSensor(SensorPort.S2);
		UltrasonicSensor six = new UltrasonicSensor(SensorPort.S3);

		LCD.drawString("Waiting...", 0, 2);

		try{
				
			NXTConnection con = RS485.getConnector().waitForConnection(0, NXTConnection.RAW);
//			BTConnection link;
			
//			link = Bluetooth.waitForConnection();
//			DataOutputStream dos = link.openDataOutputStream();
			
			LCD.drawString("Connected", 0, 2);
			
			DataOutputStream dos = con.openDataOutputStream();
			int ID = 0;

			while (Button.ESCAPE.isUp()){
				//LCD.clear();
				//LCD.drawString("Continue...", 0, 4);
				dos.flush();

				if (four.getDistance() < 11 && ID != 4) {
					Sound.beep();
					LCD.drawString("Writing", 0, 4);
					dos.writeInt(4);						
					LCD.drawInt(4, 0, 3);
					ID = 4;
				}
				if (five.getDistance() < 11 && ID != 5) {
					Sound.beep();
					LCD.drawString("Writing", 0, 4);
					dos.writeInt(5);
					LCD.drawInt(5, 0, 3);
					ID = 5;
				}
				if (six.getDistance() < 11 && ID != 6) {
					Sound.beep();
					LCD.drawString("Writing", 0, 4);
					dos.writeInt(6);
					LCD.drawInt(6, 0, 3);
					ID = 6;
				}
				
				LCD.drawInt(0, 0, 3);
				LCD.drawString("Writing 0's", 0, 4);
				dos.writeInt(0);
				dos.close();
				LCD.clear();
				
			}//loop
			
			dos.close();
//			link.close();
			con.close();
		} catch (Exception e){
			Sound.buzz();
			LCD.drawString("Write Error", 0, 5);
			System.exit(1);
		}//catch
		
	}//main
	
}//class