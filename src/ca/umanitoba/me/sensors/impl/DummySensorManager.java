package ca.umanitoba.me.sensors.impl;
/**
 * 
 */


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import ca.umanitoba.me.sensors.SensorListener;
import ca.umanitoba.me.sensors.SensorManager;

/**
 * @author Paul
 *
 */
public class DummySensorManager implements SensorManager {
	
	private static final String[] SENSOR_STRINGS = new String[] {
		"sensor1",
		"sensor2",
		"sin",
		"sensor_0"
	};
	
	private double[] sensorData = new double[4];
	
	private long startTime;
	
	private Map<String, Set<SensorListener>> listenersMap;
	
	private Timer sensorReadingsTimer;
	
	private DummySensorManager()
	{		
		listenersMap = new HashMap<String, Set<SensorListener>>();
	}
	
	private static DummySensorManager theInstance;
	
	/** 
	 * Return a sensormanager
	 */
	public static synchronized SensorManager getTheInstance()
	{
		if (null == theInstance)
		{
			theInstance = new DummySensorManager();
		}
		
		return theInstance;
	}



	@Override
	public void startSensors() 
	{
		startTime = System.currentTimeMillis();
		// simulate sensor readings
		sensorReadingsTimer = new Timer("Sensor update Timer");
		
		TimerTask readSensorsTask = new TimerTask() {
			
			@Override
			public void run() {
				
				//synchronized (sensorData) {
					double time = (System.currentTimeMillis() - startTime) / 1000.0;
					
					sensorData[0] = (float)Math.random();
					sensorData[1] = (float)(2*Math.random() + 1);
					sensorData[2] = (float)Math.cos(2 * Math.PI * 2 * time);
					sensorData[3] = 0;
					
					notifyListeners("sin", sensorData[2]);
				//}			
			}
		};
		
		sensorReadingsTimer.scheduleAtFixedRate(readSensorsTask, 0, 5);
	}


	private void notifyListeners(String sensorString, double value)
	{
		
		Set<SensorListener> listeners = listenersMap.get(sensorString);
		
		if (null != listeners) 
		{
			for (SensorListener sensorListener : listeners) 
			{
				System.out.println("notify for sensor " + sensorString);
				sensorListener.sensorChanged(value, sensorString);
			}
		}
		
	}

	@Override
	public void closeSensors() 
	{
		sensorReadingsTimer.cancel();
		System.out.println("Stopped sensor data");
	}

	@Override
	public void addListener(SensorListener listener, String sensorID) {
		Set<SensorListener> listenersForSensor = listenersMap.get(sensorID);
		
		if (null == listenersForSensor)
		{
			listenersForSensor = new HashSet<SensorListener>();
		}
		
		listenersForSensor.add(listener);
		
		listenersMap.put(sensorID, listenersForSensor);
	}

	@Override
	public void removeListener(SensorListener listener, String sensorID) {
		Set<SensorListener> listenersForSensor = listenersMap.get(sensorID);
		
		if (null == listenersForSensor)
		{
			listenersForSensor = new HashSet<SensorListener>();
		}
		
		if(listenersForSensor.remove(listener))
		{
			System.out.println("removed a listener for " + sensorID);
			listenersMap.put(sensorID, listenersForSensor);	
		}
	}

	@Override
	public void addListener(SensorListener listener, String sensorID, double value) 
	{
		System.err.println("addListener(SensorListener listener, String sensorID, double value) has not yet been implemented.");
	}

	@Override
	public void removeListener(SensorListener listener, String sensorID, double value) {
		System.err.println("addListener(SensorListener listener, String sensorID, double value) has not yet been implemented.");
	}
	
	@Override
	public double[] getSensorData() 
	{
			return sensorData.clone();
	}
	@Override
	public String[] getSensorNames() {
		return SENSOR_STRINGS;
	}
}
