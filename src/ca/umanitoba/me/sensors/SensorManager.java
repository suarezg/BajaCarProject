package ca.umanitoba.me.sensors;

/**
 * An interface to hide the implementation details of the car's sensors.
 * @author Paul
 *
 */
public interface SensorManager 
{
	/**
	 * Initilialize sensors and begin gathering data. Call after constructing.
	 */
	public void startSensors();
	
	/**
	 * Release sensors and stop gathering data. Call before destructing.
	 */
	public void closeSensors();
	
	/**
	 * Subscribe to receive events from a particular sensor type.
	 * @param sensorType An identifier for the desired sensor (e.g. <code>"TouchSensor0"</code>).
	 * Valid identifiers must be provided by the implementation. Also call {@link SensorManager#getSensorNames()}.
	 */
	public void addListener(SensorListener listener, String sensorID);
	
	/**
	 * Unsubscribe to receive events from a particular sensor type.
	 * @param sensorType An identifier for the desired sensor (e.g. <code>"TouchSensor0"</code>).
	 * Valid identifiers must be provided by the implementation. Also call {@link SensorManager#getSensorNames()}.
	 */
	public void removeListener(SensorListener listener, String sensorID);
	
	/**
	 * Subscribe to receive events from a particular sensor type when it reaches a certain value.
	 * @param sensorType An identifier for the desired sensor (e.g. <code>"TouchSensor0"</code>).
	 * Valid identifiers must be provided by the implementation. Also call {@link SensorManager#getSensorNames()}.
	 * @param value the 'trigger' value to look for
	 */
	public void addListener(SensorListener listener, String sensorID, double value);
	
	/**
	 * Unsubscribe to receive events from a particular sensor type when it reaches a certain value.
	 * @param sensorType An identifier for the desired sensor (e.g. <code>"TouchSensor0"</code>).
	 * Valid identifiers must be provided by the implementation. Also call {@link SensorManager#getSensorNames()}.
	 * @param value the 'trigger' value to look for
	 */
	public void removeListener(SensorListener listener, String sensorID, double value);
	
	/**
	 * @return the most recent values all the different sensors. Each index corresponds to a 
	 * different sensor, and corresponds to the sensor names returned by {@link #getSensorNames()}.
	 */
	public double[] getSensorData();

	/**
	 * @return an array of <code>String</code>s where the <code>String</code> at each index contains the name
	 * of a sensor. These names should correspond to the data in the array returned by {@link #getSensorData()}.
	 */
	public String[] getSensorNames();


}
