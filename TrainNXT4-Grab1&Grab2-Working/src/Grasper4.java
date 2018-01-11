// BRICK program - Spring 2015, Fall 2017
// Cody Martin

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import lejos.nxt.Button;
import lejos.nxt.LCD;
import lejos.nxt.LightSensor;
import lejos.nxt.Motor;
import lejos.nxt.SensorPort;
import lejos.nxt.Sound;
import lejos.nxt.UltrasonicSensor;
import lejos.nxt.comm.USB;
import lejos.nxt.comm.USBConnection;
import lejos.util.Timer;
import lejos.util.TimerListener;

public class Grasper4 implements TimerListener{

	static int downCart = 67; // 70 originally
	
	static int downHopper = 56; //63
	
	static boolean gate = false;
	
	static int num_blocks = 0;
	
	static double time;

	static LightSensor light = new LightSensor(SensorPort.S1);
	
	static UltrasonicSensor sound = new UltrasonicSensor(SensorPort.S2);
	
	static Timer timer = new Timer(100, new Grasper4());

	static DataOutputStream toPC;

	static DataInputStream fromPC;
	
	public static void main(String[] args) throws IOException {

		/*
		 * 
		 * //Motor guide
		 * 
		 * //Grabbing motor-a
		 * 
		 * //Up and down motor - b
		 * 
		 * //rotation motor-c
		 * 
		 * 
		 * 
		 * 
		 * 
		 * turn clockwise
		 * 
		 * Motor.C.rotate(-180);
		 * 
		 * Button.waitForAnyPress();
		 * 
		 * turn counterclockwise
		 * 
		 * Motor.C.rotate(360);
		 * 
		 * Button.waitForAnyPress();
		 * 
		 * Motor.C.rotate(-180);
		 * 
		 * Button.waitForAnyPress();
		 * 
		 * 
		 * 
		 * up and down
		 * 
		 * goes down
		 * 
		 * Motor.B.rotate(50);
		 * 
		 * Button.waitForAnyPress();
		 * 
		 * goes up
		 * 
		 * Motor.B.rotate(-50);
		 * 
		 * Button.waitForAnyPress();
		 * 
		 * 
		 * 
		 * closes
		 * 
		 * Motor.A.rotate(-45);
		 * 
		 * Button.waitForAnyPress();
		 * 
		 * opens
		 * 
		 * Motor.A.rotate(45);
		 * 
		 * Button.waitForAnyPress();
		 */

		// Initialize the light sensor

		// Give the arm a sonic sensor

		Motor.A.setSpeed(110);

		Motor.B.setSpeed(80);

		Motor.C.setSpeed(900);

		// Arm needs to send a signal and use the arm when its sensor is hit.

		// should be constantly sending 0, until arm is hit, then it sends 1's
		// while sensor is waiting for trigger.

		// Once the sensor is triggered, the arm comes down

		// Make a connection

		USBConnection link;

		System.out.println("Wait for Master");

		link = USB.waitForConnection();

		toPC = link.openDataOutputStream();

		fromPC = link.openDataInputStream();

		System.out.println("Connected to Master");

		LCD.clear();
		// Value of trigger determines action: 1 unload, 2 load
		int trigger = fromPC.readInt();

		while (!Button.ESCAPE.isDown() && trigger != 3) {
			num_blocks = 0;
			
			if(trigger != 0){
				while (!(sound.getDistance() < 7)) {
					toPC.writeInt(0);
					gate = true;
				}
			 
				toPC.writeInt(1);
				toPC.flush();
				Sound.twoBeeps();
				if (gate) {
					if(trigger == 1)
						unload();
					else
						load();
					gate = false;
				}//mini if
			
			}// if trigger
			
			trigger = fromPC.readInt();
		}//outer loop
		
		toPC.close();
		fromPC.close();
	}// main 

	public static void unload() throws IOException {
		time = 0;
		LCD.clear();
	
		timer.start();
		
		for(int i = 0; i < 6 && num_blocks < 4; i++){
			
			// Take block from cart
			Motor.B.rotate(downCart);
			
	
			if(search2Unload()){// if block is found
				// Put block into hopper
				Motor.C.rotate(580 + num_blocks * 73); // originally 560 + ...
				Motor.B.rotate(downHopper);
				Motor.A.rotate(60);
				Motor.B.rotate(-downHopper);	
				
				// Return to start position
				Motor.C.rotate(-580 - num_blocks * 73); // originally -560 - ...
				
				num_blocks++;
			}
			else{
				Motor.B.rotate(-downCart);
				break;
			}
		}// end loop
		timer.stop();
		
		// Signal Master brick
		toPC.writeInt(1);
		toPC.flush();
	
		LCD.drawString(time + "", 0, 0);

	}// unload()
	
	
	public static void load() throws IOException{
		time = 0;
		LCD.clear();
		
		timer.start();
		
		for(int i = 0; i < 6 && num_blocks < 4; i++){
			
			// Take block from hopper
			Motor.C.rotate(580); // originally 520
			
			Motor.B.rotate(downHopper);
	
			if(search2Load()){// if block is found
				
				num_blocks++;

				// Put block into cart
				Motor.C.rotate(-480 - num_blocks * 73); // originally -520
				Motor.B.rotate(downCart); // originally downCart - 3
				Motor.A.rotate(60);
				Motor.B.rotate(-downCart);	// originally -downCart + 3
				Motor.C.rotate(-100);
				// Return to start position
				Motor.C.rotate(num_blocks * 73);
				
			}
			else{
				Motor.B.rotate(-downHopper);
				Motor.C.rotate(-580); // originally 520
				break;
			}
		}// end loop
		
		timer.stop();
		
		// Signal Master brick
		toPC.writeInt(1);
		toPC.flush();
		
		LCD.drawString(time + "", 0, 0);
	}// load()
	
	
	// Method moves arm over search area
	// Returns true if found block
	public static boolean search2Unload(){
		int degree = 40;
		int l_value;
//		Motor.C.rotate(degree*3); // newly added
		
		for(int i = 0; i < 10; i++){
			
			l_value = light.getLightValue();
			LCD.drawString(l_value + "", 0, 0);
			
			if(l_value >= 49){
				Motor.A.rotate(-60);
				Motor.B.rotate(-downCart);
				
				// Double check that block has actually been grabbed
				l_value = light.getLightValue();
				LCD.drawString(l_value + "", 0, 2);
				
				// Move arm back to start position
//				if(i > 3) {
//					Motor.C.rotate((degree) * (i-3)); //was -degree
//				}
//				else {
					Motor.C.rotate((degree) * i); //was -degree
//				}
				
				
				if(l_value < 35){// Mitigation for when gripper drops/fails to grab block -- originally < 45
					Motor.A.rotate(60);
					//return false;
					Motor.B.rotate(downCart);
					return search2Unload();// Not really a recursive call, just a "redo"
				}
				else
					return true;
				
			}
			
			Motor.C.rotate(-degree);

		}// end loop
		
		Motor.C.rotate(degree * 6);
		return false;
	}// search2Unload()

	
	// Method moves arm over search area
	// Returns true if found block
	public static boolean search2Load(){
		int degree = 40;
		int l_value;
			
		for(int i = 0; i < 9; i++){
				
			l_value = light.getLightValue();
			
			if(l_value >= 45){ // originally >=52
				Motor.A.rotate(-60);
				Motor.B.rotate(-downHopper);
					
				// Double check that block has actually been grabbed
				l_value = light.getLightValue();
					
				// Move arm back to start position
				Motor.C.rotate(-degree * i); 
					
				if(l_value < 35){// Mitigation for when gripper drops/fails to grab block -- originally < 45
					Motor.A.rotate(60);
					//return false;
					Motor.B.rotate(downHopper);
					return search2Load();// Not really a recursive call, just a "redo"
				}
				else
					return true;
			}
				
			Motor.C.rotate(degree);

		}// end loop
			
		Motor.C.rotate(-degree * 9);
		return false;
	}// search2Load()

	@Override
	public void timedOut() {
		time = time + 0.1;
	}
		
}// grasper class