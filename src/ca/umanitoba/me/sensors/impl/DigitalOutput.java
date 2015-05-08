package ca.umanitoba.me.sensors.impl;

import com.phidgets.PhidgetException;

/**
 * Support writing to digital output for setting multiplexer channel
 * @author Paul
 *
 */
public class DigitalOutput 
{
	public static final int MUX_PIN = 1;
	public static final int SERVER_READY_LED = 0;
	
	
	public static void setAutomaticControl(boolean isAutomatic)
	{
		System.out.println("Set auto control: " + isAutomatic);
		try {
			BajaSensorManager.getBajaSensorManagerInstance().setGPIO(MUX_PIN, isAutomatic);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			System.err.println("Exception caught. Continuing.");
			e.printStackTrace();
		}
	}
	
	public static void setIsReadyLED(boolean isServerReady)
	{
		System.out.println("Set server ready LED: " + isServerReady);
		try {
			BajaSensorManager.getBajaSensorManagerInstance().setGPIO(SERVER_READY_LED, isServerReady);
		} catch (PhidgetException e) {
			System.err.println("Caught Exception. continuing.");
			e.printStackTrace();
		}
	}

}
