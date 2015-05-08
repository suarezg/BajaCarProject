/**
 * 
 */
package ca.umanitoba.me.carcontroller.impl;

import ca.umanitoba.me.carcontroller.SpeedController;
import ca.umanitoba.me.carcontroller.SteeringController;

/**
 * This class can be used for testing in place of the Speed/Steering controllers returned by {@link ca.umanitoba.me.carcontroller.ServoManager ServoManager}
 * by instantiating it with a default constructor (i.e. call <code>new DummySpeedSteeringController()</code>).
 * @author Paul
 *
 */
public class DummySpeedSteeringController implements SpeedController,
		SteeringController {

	/* (non-Javadoc)
	 * @see ca.umanitoba.me.carcontroller.SteeringController#setSteeringAngle(float)
	 */
	@Override
	public void setSteeringAngle(float angle)
	{
		System.out.println("Set angle " + angle);
	}

	/* (non-Javadoc)
	 * @see ca.umanitoba.me.carcontroller.SpeedController#turnOff()
	 */
	@Override
	public void turnOff() 
	{
		System.out.println("speed off");
	}

	/* (non-Javadoc)
	 * @see ca.umanitoba.me.carcontroller.SpeedController#turnOn()
	 */
	@Override
	public void turnOn()
	{
		System.out.println("speed on");
	}

	/* (non-Javadoc)
	 * @see ca.umanitoba.me.carcontroller.SpeedController#setCarSpeed(float)
	 */
	@Override
	public void setCarSpeed(float targetSpeed) 
	{
		System.out.println("set speed " + targetSpeed);
	}

	@Override
	public void setCalibrationOffset(double offset) 
	{
		System.out.println("calibrate: " + offset);
	}

}
