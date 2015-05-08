package ca.umanitoba.me.carcontroller.impl;

import ca.umanitoba.me.carcontroller.SpeedController;
import ca.umanitoba.me.sensors.SensorListener;
import ca.umanitoba.me.sensors.SensorManager;
import ca.umanitoba.me.sensors.impl.BajaSensor;
import ca.umanitoba.me.sensors.impl.BajaSensorManager;
import ca.umanitoba.me.sensors.impl.DummySensorManager;

import java.util.Date;


/**
 * This class acts as a speed controller for the Baja vehicle.
 * By setting and instance of this classes speed with the method setCarSpeed()
 * the object will send new speed settings to the servo controller to maintain
 * the desired speed. This class can be used as an open loop controller if maximum
 * outputs are limited to the servo controller externally. The call can utilize
 * proportional, integral, and derivative control; only proportional and integral
 * control are currently active.
 * @author rhyse
 */
public class ClosedLoopSpeedController implements SpeedController, SensorListener {

	//constants
	private static final String sensorString = BajaSensor.LongitudinalSpeed.toString();
	public static final double WHEEL_RADIUS = 0.084217; // in meters
	public static final double GEAR_RATIO = 8.05; // ratio between motor RPM and wheel RPM
	public static final double PI= Math.PI;

	//instance fields
	private double targetMotorSpeed = 0; // meters per second
	private volatile boolean isRunning = false; // volatile for thread sharing
	private double encoder=0;
	private double calibrationOffset = 0;

	SensorManager sensor;  //exchange for actual

	private final int Kp=1;  //Proportional contant
	private final int Ki=2;  //inegral constant
	//int Kd=2;  Derivative control not currently used

	//Control Variables
	private double preTime=0; //previous time
	private double err=0;
	private double timeStep=0; //time between calculations
	private double intErr=0;  //total integral error to date
	//private double devErr=0;
	//private double preErr=0;
	private double condErr=0;  //speed value sent to motor controller

	/* Public API */

	/**
	 * Initialize the <code>SpeedController</code>.
	 * @param test
	 */

	//contructor
	protected ClosedLoopSpeedController(SensorManager test)
	{
		sensor = test;
	}

	@Override
	public void turnOff()
	{
		isRunning = false;
		sensor.removeListener(this, sensorString);
		ServoManager.setSpeed(0);
	}

	@Override
	public void turnOn()
	{

		init();
		isRunning = true;
		preTime = 0;
		sensor.addListener(this, sensorString);
	}

	private void init()
	{
		//Control Variables
		preTime=0; //previous time
		err=0;
		timeStep=0; //time between calculations
		intErr=0;  //total integral error to date
		condErr=0;  //speed value sent to motor controller

		targetMotorSpeed = 0; // meters per second
		isRunning = false;
		encoder=0;
		calibrationOffset = 0;
	}

	@Override
	public void setCarSpeed(float targetSpeed)
	{
		this.targetMotorSpeed = targetSpeed + calibrationOffset;
		
		//refreshCarSpeed(); // or try calling ServoManager.setSpeed() directly
		ServoManager.setSpeed(targetSpeed / (2 * PI * WHEEL_RADIUS));
	}

	@Override
	public void setCalibrationOffset(double offset) 
	{
		this.calibrationOffset = offset;
	}

	@Override
	public void sensorChanged(double value, String sensorType) 
	{
		System.out.println("Sensor: " + sensorType + " changed to " + value);
		if (isRunning && sensorType.equals(sensorString)) {
			this.encoder=value;
			refreshCarSpeed();
		}
	}

	private void refreshCarSpeed () {


		//Error Calculation
		err = (this.targetMotorSpeed-this.encoder);

		//Integral Calculation
		if(preTime == 0)
		{
			preTime = new Date().getTime(); // initialize to now
		}

		timeStep = ((double)new Date().getTime()-preTime)/1000;
		preTime = new Date().getTime();
		intErr += (double)timeStep*err;

		//Derivative Calculation
		//        if (timeStep==0) 
		//        {
		//            devErr=0;
		//        }
		//        else 
		//        {
		//            devErr=(err-preErr)/timeStep;
		//        }
		//        preErr=err;

		//Conditioned Error Calculation
		condErr=Kp*err+Ki*intErr;//Kd*devErr+;
		System.out.println("conditionedError: "+ condErr);
		System.out.println("error: " + err);
		System.out.println("interr: " + intErr);
		//convert to rev/s of wheel and send
		
		if (isRunning)
		{
			ServoManager.setSpeed(condErr/(2*PI*WHEEL_RADIUS));
		}
	}
}
