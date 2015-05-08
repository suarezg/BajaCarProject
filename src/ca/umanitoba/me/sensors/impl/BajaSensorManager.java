package ca.umanitoba.me.sensors.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLongArray;

import ca.umanitoba.me.sensors.SensorListener;
import ca.umanitoba.me.sensors.SensorManager;

import com.phidgets.EncoderPhidget;
import com.phidgets.InterfaceKitPhidget;
import com.phidgets.PhidgetException;
import com.phidgets.SpatialEventData;
import com.phidgets.SpatialPhidget;
import com.phidgets.event.AttachEvent;
import com.phidgets.event.AttachListener;
import com.phidgets.event.DetachEvent;
import com.phidgets.event.DetachListener;
import com.phidgets.event.EncoderPositionChangeEvent;
import com.phidgets.event.EncoderPositionChangeListener;
import com.phidgets.event.SpatialDataEvent;
import com.phidgets.event.SpatialDataListener;

/**
 * This class interfaces with all the sensors that are physically attached to the vehicle.
 * 
 * <p>It implements the interfaces needed to deal with Phidgets(r) sensors. If you want to
 * use a different brand of sensor, only this class will need to be replaced.
 * 
 * <p>If you would like to add additional (Phidgets) sensors, you can add necessary code
 * in the areas marked with 'TODO'.
 * 
 * <p>When you receive updated sensor data, make sure that you update the local sensor
 * data buffer with the following code:
 * 
 * <p><code>sensorDataVolatile.set(BajaSensor.</code>&lt<i>sensor name</i>&gt<code>.getIndex(), Double.doubleToLongBits(</code>&lt<i>value</i>&gt<code>));</code>
 * 
 * @author Paul
 *
 */
public class BajaSensorManager 

implements 
//InputChangeListener,
//SensorChangeListener,
SensorManager, 
AttachListener, 
DetachListener, 
EncoderPositionChangeListener, 
SpatialDataListener 
{	
	public static final double WHEEL_RADIUS = 0.09; // m
	static final boolean SENSOR_LISTENERS_ENABLED = true;
	private static final int SPATIAL_DATA_RATE = 40; // ms for sampling rate for PhidgetSpatial.
	
	private AtomicLongArray sensorDataVolatile = new AtomicLongArray(BajaSensor.values().length);
	private String[] sensorNameStrings;
		
	private Map<String, Set<SensorListener>> listenersMap;
	
	
	// Phidgets sensor objects
	private SpatialPhidget mSpatialPhidget;
	private EncoderPhidget mEncoderPhidget;
	private InterfaceKitPhidget mInterfaceKitPhidget;
	
	//TODO: References for other sensors
	
	private boolean isSensing;
	
	//private SensorFusion sensorFusion;
	
	/** 
	 * Retreive the instance of BajaSensorManager to interface with the sensors. This is different from
	 * {@link #getTheInstance()} because it returns a BajaSensorManager. This provides access to the Phidgets-based
	 * implementation details. (specifically the GPIO afforded by the InterfaceKitPhidget)
	 */
	static synchronized BajaSensorManager getBajaSensorManagerInstance()
	{
		if (null == theInstance)
		{
			theInstance = new BajaSensorManager();
		}
		
		return theInstance;
	}
	
	/**
	 * Returns an instance of {@link ca.umanitoba.me.sensors.SensorManager SensorManager} that can be used with 
	 * the Baja vehicle.
	 */
	public static SensorManager getTheInstance()
	{
		return getBajaSensorManagerInstance();
	}
	
	private static BajaSensorManager theInstance = null;
	
	private BajaSensorManager()
	{		
		listenersMap = new HashMap<String, Set<SensorListener>>();
		startSensors(); // connect to sensors right away
	}
	
	/**
	 * Necessary calls to initialize the various sensors and actuators that we will use
	 * and registers for callbacks when the phidgets have attached/detached. Note that
	 * this will lock the phidgets until {@link #closePhidgets()} is called.
	 * @throws PhidgetException 
	 */
	private void initializePhidgets() throws PhidgetException
	{
		// initialze Spatial
		mSpatialPhidget = new SpatialPhidget();
		mSpatialPhidget.openAny(); // only one accelerometer is used so we can use openAny.
		
		mSpatialPhidget.addAttachListener(this);		
		mSpatialPhidget.addDetachListener(this);		
		
		// initialize Encoder
		mEncoderPhidget = new EncoderPhidget();
		mEncoderPhidget.openAny();
		
		mEncoderPhidget.addAttachListener(this);
		mEncoderPhidget.addDetachListener(this);
		
		// initialize InterfaceKit
		mInterfaceKitPhidget = new InterfaceKitPhidget();
		mInterfaceKitPhidget.openAny();
		
		mInterfaceKitPhidget.addAttachListener(this);
		mInterfaceKitPhidget.addDetachListener(this);
		
		//TODO: add initialization for other types of sensors
	}
	
	/**
	 * Close phidgets and disconnect from them. This allows other applications to use them.
	 * @throws PhidgetException
	 */
	private void closePhidgets() throws PhidgetException 
	{
		mSpatialPhidget.close();
		mEncoderPhidget.close();
		mInterfaceKitPhidget.close();

		//TODO: Close down other sensors
	}
	
	/**
	 * Update the IMU fields of the local private variable <code>sensorData</code> 
	 * so that it can be polled by any clients.
	 * @param accelData data to place in the Acceleration X/Y/Z fields
	 * @param gyroData data to place in the Gyroscope X/Y/Z fields
	 * @param compassData data to place in the Compass X/Y/Z fields
	 */
	private void updateIMUSensorData(double[] accelData, double[] gyroData, double[] compassData)
	{		
		if (3 == accelData.length)
		{
			sensorDataVolatile.set(BajaSensor.AccelerometerX.getIndex(), Double.doubleToLongBits(accelData[0]));
			sensorDataVolatile.set(BajaSensor.AccelerometerY.getIndex(), Double.doubleToLongBits(accelData[1]));
			sensorDataVolatile.set(BajaSensor.AccelerometerZ.getIndex(), Double.doubleToLongBits(accelData[2]));
		}
		
		if (3 == gyroData.length)
		{
			sensorDataVolatile.set(BajaSensor.GyroX.getIndex(), Double.doubleToLongBits(gyroData[0]));
			sensorDataVolatile.set(BajaSensor.GyroY.getIndex(), Double.doubleToLongBits(gyroData[1]));
			sensorDataVolatile.set(BajaSensor.GyroZ.getIndex(), Double.doubleToLongBits(gyroData[2]));
		}
		
		if (3 == compassData.length)
		{
			sensorDataVolatile.set(BajaSensor.CompassX.getIndex(), Double.doubleToLongBits(compassData[0]));
			sensorDataVolatile.set(BajaSensor.CompassY.getIndex(), Double.doubleToLongBits(compassData[1]));
			sensorDataVolatile.set(BajaSensor.CompassZ.getIndex(), Double.doubleToLongBits(compassData[2]));
		}
		
		
		if (SENSOR_LISTENERS_ENABLED) 
		{			
			if (3 == accelData.length)
			{
				notifyListeners(BajaSensor.AccelerometerX, accelData[0]);
				notifyListeners(BajaSensor.AccelerometerY, accelData[1]);		
				notifyListeners(BajaSensor.AccelerometerZ, accelData[2]);
			}
			
			if (3 == gyroData.length)
			{			
				notifyListeners(BajaSensor.GyroX, gyroData[0]);
				notifyListeners(BajaSensor.GyroY, gyroData[1]);
				notifyListeners(BajaSensor.GyroZ, gyroData[2]);
			}
			
			if (3 == compassData.length)
			{
				notifyListeners(BajaSensor.CompassX, compassData[0]);
				notifyListeners(BajaSensor.CompassY, compassData[1]);
				notifyListeners(BajaSensor.CompassZ, compassData[2]);	
			}
		}	
	}
	
	private void updateSpeedSensorData(int wheelIndex, double speed)
	{
		sensorDataVolatile.set(BajaSensor.WheelFRSpeed0.getIndex() + wheelIndex, Double.doubleToLongBits(speed));
		
		// update average speed
		double averageSpeed = 0;
		BajaSensor[] wheelSpeedSensors = BajaSensor.wheelSpeedSensors();
		
		for (BajaSensor wheelSensor : wheelSpeedSensors) 
		{
			averageSpeed += Double.longBitsToDouble(sensorDataVolatile.get(wheelSensor.getIndex()));
		}
		averageSpeed /= wheelSpeedSensors.length;
		sensorDataVolatile.set(BajaSensor.LongitudinalSpeed.getIndex(), Double.doubleToLongBits(averageSpeed));
		
		// notify listeners
		if (SENSOR_LISTENERS_ENABLED)
		{
			notifyListeners(BajaSensor.LongitudinalSpeed, averageSpeed);
			switch (wheelIndex)
			{
			case 0:
				notifyListeners(BajaSensor.WheelFRSpeed0, speed);
				break;
				
			case 1:
				notifyListeners(BajaSensor.WheelFLSpeed1, speed);
				break;
				
			case 2:
				notifyListeners(BajaSensor.WheelBRSpeed2, speed);
				break;
				
			case 3:
				notifyListeners(BajaSensor.WheelBLSpeed3, speed);
				break;

			default:
				break;
			}
			
		}
	}
	
	private void notifyListeners(BajaSensor sensor, double value)
	{
		String sensorString = sensor.toString();
		Set<SensorListener> listeners = listenersMap.get(sensorString);
		if (null != listeners) 
		{
			for (SensorListener sensorListener : listeners) 
			{
				sensorListener.sensorChanged(value, sensorString);
			}
		}
	}

	@Override
		public void attached(AttachEvent ae) 
		{
			// was Spatial attached?
			if (ae.getSource().equals(mSpatialPhidget)) {
				// configure Spatial
				mSpatialPhidget.addSpatialDataListener(this);
				System.out.println("Attached spatial");
				try {
					mSpatialPhidget.setDataRate(SPATIAL_DATA_RATE);
					System.out.println("rate: " + mSpatialPhidget.getDataRateMin() + " - " + mSpatialPhidget.getDataRate() + " - " + mSpatialPhidget.getDataRateMax());
				} catch (PhidgetException e) 
				{
					e.printStackTrace();
				}
			} 
			
			// was Encoder attached?
			else if(ae.getSource().equals(mEncoderPhidget))
			{
				// configure encoder details
				try {
					mEncoderPhidget.setEnabled(0, true);
					mEncoderPhidget.setEnabled(1, true);
					mEncoderPhidget.setEnabled(2, true);
					mEncoderPhidget.setEnabled(3, true);
					System.out.println("Enabled encoders");
				} catch (PhidgetException e) {
					System.err.println("Could not enable one or more encoders: " + e);
				}			
				
				mEncoderPhidget.addEncoderPositionChangeListener(this);
			}
			
			// was InterfaceKit attached?
			else if(ae.getSource().equals(mInterfaceKitPhidget))
			{ 
				// configure InterfaceKit http://www.phidgets.com/docs/1018_User_Guide
	//			mInterfaceKitPhidget.addInputChangeListener(this); // listen on digital inputs
	//			mInterfaceKitPhidget.addSensorChangeListener(this); // listen on analog inputs
	//			
	//			try {
	//				mInterfaceKitPhidget.setRatiometric(false); // use precision reference
	//				mInterfaceKitPhidget.setDataRate(0, mInterfaceKitPhidget.getDataRateMax(0)); // set the logging rate for the analog sensor (input 0)
	//			} catch (PhidgetException e) {
	//				e.printStackTrace();
	//			}
			}
			
		}

	//	@Override
	//	public void inputChanged(InputChangeEvent ae) 
	//	{
	//		
	//	}
	//	
	//	@Override
	//	public void sensorChanged(SensorChangeEvent ae) 
	//	{
	//		
	//	}
	
		@Override
		public void detached(DetachEvent ae) 
		{
			if (ae.getSource().equals(mSpatialPhidget)) {
				mSpatialPhidget.removeSpatialDataListener(this);;
			} else if(ae.getSource().equals(mEncoderPhidget))
			{
				mEncoderPhidget.removeEncoderPositionChangeListener(this);
			}		
		}

	@Override
	public void data(SpatialDataEvent ae) 
	{
		SpatialEventData[] data = ae.getData();
		int lastDatum = data.length - 1;
		
		double[] accelData = data[lastDatum].getAcceleration();
		double[] gyroData = data[lastDatum].getAngularRate();
		double[] compassData = data[lastDatum].getMagneticField();
		
		
		
		// update sensorData array
		updateIMUSensorData(
			accelData, 
			gyroData, 
			compassData
		);
	}

	@Override
	public void encoderPositionChanged(EncoderPositionChangeEvent ae) 
	{
		int wheelIndex = ae.getIndex();
		double speed = ((976.5625f) * ae.getValue()) / ae.getTime(); // 976.5625 = ((1000000 useconds/second) / (256steps/rotation)) / (4pulses/step)
		
		speed *= (2 * Math.PI * WHEEL_RADIUS);
		updateSpeedSensorData(wheelIndex, speed);
	}
	

//	@Override
//	public void inputChanged(InputChangeEvent ae) 
//	{
//		
//	}
//	
//	@Override
//	public void sensorChanged(SensorChangeEvent ae) 
//	{
//		
//	}

	/**
	 * Set the state of a GPIO pin
	 * @param pinIndex Index on InterfaceKit to set
	 * @param state <code>true</code> for logic high, <code>false</code> for logic low
	 * @throws PhidgetException if something didn't work
	 */
	synchronized void setGPIO(int pinIndex, boolean state) throws PhidgetException
	{
		if (!mInterfaceKitPhidget.isAttached()) 
		{
			mInterfaceKitPhidget.waitForAttachment(200);
		}
		mInterfaceKitPhidget.setOutputState(pinIndex, state);
	}

	@Override
	public synchronized void startSensors()
	{
		if(!isSensing)
		{
			try 
			{
				initializePhidgets();
				
				
				isSensing = true;
			} catch (PhidgetException e) 
			{
				e.printStackTrace();
			}
		}		
	}

	@Override
	public synchronized void closeSensors()
	{
		if (isSensing)
		{
			try {
				closePhidgets();
				
				isSensing = false;
			} catch (PhidgetException e) {
				e.printStackTrace();
			}
		}		
	}

	@Override
	public void addListener(SensorListener listener, String sensorID)
	{
		Set<SensorListener> listenersForSensor = listenersMap.get(sensorID);
		
		if (null == listenersForSensor)
		{
			listenersForSensor = new HashSet<SensorListener>();
		}
		
		if(listenersForSensor.add(listener))
		{
			System.out.println("Added a listener to sensor " + sensorID + " total: " + listenersForSensor.size());
			listenersMap.put(sensorID, listenersForSensor);	
		}
		
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
			System.out.println("removed a listener for " + sensorID + " total: " + listenersForSensor.size());
			listenersMap.put(sensorID, listenersForSensor);	
		}
	}

	@Override
	public void addListener(SensorListener listener, String sensorID, double value) 
	{
		System.err.println("addListener(SensorListener listener, String sensorID, double value) has not yet been implemented.");
	}

	@Override
	public void removeListener(SensorListener listener, String sensorID,
			double value) {
		System.err.println("addListener(SensorListener listener, String sensorID, double value) has not yet been implemented.");
	}

	@Override
	public String[] getSensorNames()
	{
		if (null == sensorNameStrings) 
		{
			BajaSensor[] sensors = BajaSensor.values();
			sensorNameStrings = new String[sensors.length];
			for (int i = 0; i < sensors.length; i++) 
			{
				sensorNameStrings[i] = sensors[i].descriptionString;
			}
		}	
		return sensorNameStrings;
	}

	@Override
	public double[] getSensorData()
	{
		double[] sensorDataDouble = new double[sensorDataVolatile.length()];
		for (int index = 0; index < sensorDataVolatile.length(); index++)
		{
			sensorDataDouble[index] = Double.longBitsToDouble(sensorDataVolatile.get(index));					
		}
		
		return sensorDataDouble;
		//return sensorData.clone();
	}
}
