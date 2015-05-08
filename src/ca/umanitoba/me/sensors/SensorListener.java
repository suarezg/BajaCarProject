package ca.umanitoba.me.sensors;

/**
 * An interface to allow various pieces of the car to subscribe to sensor events
 * @author Paul
 *
 */
public interface SensorListener 
{
	/**
	 * Called by a {@link ca.umanitoba.me.sensors.SensorManager} when a sensor changes its value.
	 * @param sensorType a string identifying the sensor that changed
	 * @param value the new value. Type is based upon implementation.
	 */
	public void sensorChanged(double value, String sensorType);
}
