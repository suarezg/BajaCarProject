package ca.umanitoba.me.carcontroller;

/**
 * Allow a client to set the steering angle of the front wheels
 * @author Paul
 *
 */
public interface SteeringController 
{
	/**
	 * Set the angle for the front set of wheels.
	 * @param angle should be between -90 and + 90. Consult concrete implementaiton for details
	 */
	public void setSteeringAngle(float angle);
	
	/**
	 * Set a calibration offset. First call {@link #setSteeringAngle(float) setSteeringAngle(0)}, then 
	 * call this method with different values of <code>offset</code> until the steering wheels point straight ahead.
	 * @param offset amount by which to offset the steering angle.
	 */
	public void setCalibrationOffset(double offset);
}
