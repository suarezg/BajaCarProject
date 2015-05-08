package ca.umanitoba.me.carcontroller;

/**
 * Allow a client to set the longitudinal speed of the vehicle
 * @author Paul
 *
 */
public interface SpeedController 
{
	/**
	 * Stop the motor controller and set the speed to 0.
	 */
	public void turnOff();
	
	/**
	 * Start the motor controller.
	 */
	public void turnOn();
	
	/**
	 * Set a new speed for the controller to target.
	 * @param targetSpeed target land speed (meters/second)
	 */
	public void setCarSpeed(float targetSpeed);
	
	/**
	 * Set a calibration offset. First call {@link #setCarSpeed(float) setCarSpeed(0)}, then 
	 * call this method with different values of <code>offset</code> until the car does not move.
	 * @param offset amount by which to offset the speed.
	 */
	public void setCalibrationOffset(double offset);
}
