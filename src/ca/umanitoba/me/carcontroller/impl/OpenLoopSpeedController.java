package ca.umanitoba.me.carcontroller.impl;

import ca.umanitoba.me.carcontroller.SpeedController;


/**
 * This class allows the ESC to be controlled by the {@link ca.umanitoba.me.car.impl.BajaBackend BajaBackend}
 * to be set to a constant speed. The controller will maintain the speed set by {@link #setMotorSpeed(float)}, even
 * when the car is turning.
 * @author Paul White
 * @version 0.1
 *
 */
public class OpenLoopSpeedController implements SpeedController
{
	public static final double WHEEL_RADIUS = 0.09; // in meters
	
	public static final double GEAR_RATIO = 1; // ratio between motor RPM and wheel RPM
	
	
	private double targetMotorSpeed = 0; // meters per second
	private boolean isRunning = false;
	
	private double calibrationOffset;
	
	/* Public API */
	
	/**
	 * Initialize the <code>SpeedController</code>.
	 */
	protected OpenLoopSpeedController()
	{
		
	}
	
	@Override
	public void turnOff()
	{
		isRunning = false;
	}
	
	@Override
	public void turnOn()
	{
		isRunning = true;
	}
	
	@Override
	public void setCarSpeed(float targetSpeed)
	{
		this.targetMotorSpeed = targetSpeed/(2 * Math.PI * WHEEL_RADIUS) + calibrationOffset; // convert from m/s to revolutions /second
		ServoManager.setSpeed(this.targetMotorSpeed);
	}

	@Override
	public void setCalibrationOffset(double offset) 
	{
		this.calibrationOffset = offset;
	}
	
	/* Private methods */
	
	
}
