/**
 *
 */
package ca.umanitoba.me.carcontroller.impl;

import com.phidgets.AdvancedServoPhidget;
import com.phidgets.PhidgetException;
import com.phidgets.event.AttachEvent;
import com.phidgets.event.AttachListener;
import com.phidgets.event.DetachEvent;
import com.phidgets.event.DetachListener;
import com.phidgets.event.ServoPositionChangeEvent;
import com.phidgets.event.ServoPositionChangeListener;

import ca.umanitoba.me.carcontroller.SpeedController;
import ca.umanitoba.me.carcontroller.SteeringController;
import ca.umanitoba.me.sensors.impl.BajaSensorManager;

/**
 * @author Paul
 * @date July 11, 2014
 * @version 0.1
 *
 * Manage Phidgets servo controller and provide instance of {@link ca.umanitoba.me.carcontroller.SpeedController SpeedController} and 
 * {@link ca.umanitoba.me.carcontroller.SteeringController SteeringController} that control real live servos.
 */
public class ServoManager implements AttachListener, DetachListener, ServoPositionChangeListener
{	
	private static SteeringServoDescriptor steeringServo;
	
	private static final int SPEED_INDEX = 5; // servo 5 to control speed
	private static final int STEERING_INDEX = 0; // servo 0 to control steering
	
	// Speed curve; speed > 0
	private static final double MIN_SPEED_FWD = 4; //RPS
	private static final double SLOPE_FWD = 0.1749; // Forward slope
	private static final double OFFSET_FWD = -263.6; // Y-intercept
	
	// Speed curve; speed < 0
	private static final double MIN_SPEED_REV = -3;
	private static final double SLOPE_REV = 0.1271; // Reverse slope
	private static final double OFFSET_REV = -173.65; // Y-intercept
	
	/**
	 * use linear approximation of speed-pulse width curve to find desired pulse width
	 * @param speed
	 * @return the pulse width to send to the motor
	 */
	private static final double calculatePulseWidth(double speed)
	{
		double pulseWidth = PULSE_WIDTH_NEUTRAL_MICROS;
		double pulseWidthCalculated = PULSE_WIDTH_NEUTRAL_MICROS;
		
		if (speed > MIN_SPEED_FWD)
		{
			pulseWidthCalculated = ((speed - OFFSET_FWD) / SLOPE_FWD);
			pulseWidth = constrain(pulseWidthCalculated, MAX_SPEED_MICROS, PULSE_WIDTH_NEUTRAL_MICROS);
		} else if (speed < MIN_SPEED_REV)
		{
			pulseWidthCalculated = ((speed - OFFSET_REV) / SLOPE_REV);
			pulseWidth = constrain(pulseWidthCalculated, PULSE_WIDTH_NEUTRAL_MICROS, MAX_SPEED_REVERSE_MICROS);
		}
		
		System.out.println("pulse: " + pulseWidthCalculated);
		
		return pulseWidth;
	}
	
	private static final double MAX_SPEED_MICROS = 1800; // 2070
	private static final double PULSE_WIDTH_NEUTRAL_MICROS = 1445;
	private static final double MAX_SPEED_REVERSE_MICROS = 1006;
	
	private static ServoManager theInstance;
	private static SteeringController theSteeringController;
	private static SpeedController theSpeedController;
	
	AdvancedServoPhidget servo;
	
	private ServoManager() throws PhidgetException
	{		
		if (null == theInstance) 
		{
			steeringServo = SteeringServoDescriptor.HPI_SFL11MG;
			
			servo = new AdvancedServoPhidget();
			
			servo.addAttachListener(this);
			servo.addDetachListener(this);
			servo.addServoPositionChangeListener(this);
			
			servo.openAny();
			
			theInstance = this;
			
		}		
	}
	
	public static ServoManager getTheInstance() throws PhidgetException
	{
		if (null == theInstance)
		{
			theInstance = new ServoManager();
		}
		
		return theInstance;
	}
	
	
	/**
	 * @return a <code>SteeringController</code> to allow the steering to be controlled, or null if a problem occurred
	 * connecting to steering hardware.
	 */
	public static SteeringController getSteeringController()
	{
		try {
			getTheInstance(); // initialize if need be
			
			if (null == theSteeringController)
			{
				theSteeringController = new OpenLoopSteeringController(); // construct an open-loop controller.
				
				//TODO: initialize Rhyse's closed-loop steering controller code here instead.
			}
			return theSteeringController;
		} catch (Exception e) {
			return null;
		}
		
	}
	
	/**
	 * @return a <code>SpeedController</code> to allow the speed to be controlled, or null if a problem occurred
	 * connecting to speed hardware.
	 */
	public static SpeedController getSpeedController()
	{
		if (null == theSpeedController)
		{
			theSpeedController = new ClosedLoopSpeedController(BajaSensorManager.getTheInstance());
			
			//TODO: initialize Rhyse's closed-loop speed controller code here instead.
		}
		
		return theSpeedController;
	}
	
	/**
	 * Stop the vehicle and cancel any speed controller
	 */
	public static void haltSpeed()
	{
		theSpeedController.turnOff();
		setSpeed(0);
	}
	
	/**
	 * Set the vehicle wheel speed (Revolutions / second)
	 * @param speed value to write to the ESC
	 */
	protected static void setSpeed(double speed)
	{		
		try {
			double pulseWidth = calculatePulseWidth(speed);
			getTheInstance().servo.setEngaged(SPEED_INDEX, true);
			getTheInstance().servo.setPosition(SPEED_INDEX, pulseWidth);
			System.out.println("set servo position " + pulseWidth);
		} catch (PhidgetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets the pulse width of the servo control signal in microseconds
	 * @param speedMicros
	 */
	protected static void setSpeedMicros(double speedMicros)
	{
		try {
			getTheInstance().servo.setEngaged(SPEED_INDEX, true);
			getTheInstance().servo.setPosition(SPEED_INDEX, speedMicros);
		} catch (PhidgetException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Set the vehicle steering wheel angle. automatically clamps the angle to be within the range supported by the attached motor.
	 * @param angle
	 */
	protected static void setSteeringAngle(double angle)
	{
		double clampedAngle = angle;
		// clamp to prevent overstraining the servo
		if (clampedAngle > steeringServo.hardRightAngle)
		{
			clampedAngle = steeringServo.hardRightAngle;
		}
		
		if (clampedAngle < steeringServo.hardLeftAngle)
		{
			clampedAngle = steeringServo.hardLeftAngle;
		}		
		
		double offset = (steeringServo.hardRightAngle - steeringServo.hardLeftAngle) / 2.0;
		
		double steeringAngle = offset + angle;
			
		// set it		
		try {
			getTheInstance().servo.setPosition(STEERING_INDEX, steeringAngle);
			getTheInstance().servo.setEngaged(STEERING_INDEX, true);
			System.out.println("Set servo angle: " + steeringAngle);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/* Phidgets Callbacks */

	@Override
	public void servoPositionChanged(ServoPositionChangeEvent ae) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void detached(DetachEvent ae) 
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void attached(AttachEvent ae) 
	{
		try {
			System.out.println("Serial: " + servo.getSerialNumber());
			System.out.println("Servos: " + servo.getMotorCount());

			//Initialize the steering servo
			servo.setEngaged(STEERING_INDEX, false);
			servo.setSpeedRampingOn(STEERING_INDEX, false);
			
			servo.setServoParameters(STEERING_INDEX, 
					steeringServo.lowUS, 
					steeringServo.highUS, 
					steeringServo.fullArc, 
					steeringServo.velocityMax);
			
			// initialize the speed servo
			servo.setEngaged(SPEED_INDEX, true);
			servo.setSpeedRampingOn(SPEED_INDEX, true);
			servo.setServoType(SPEED_INDEX, AdvancedServoPhidget.PHIDGET_SERVO_RAW_us_MODE); // use raw microsecond parameters
			servo.setPosition(SPEED_INDEX, PULSE_WIDTH_NEUTRAL_MICROS);
			servo.setVelocityLimit(SPEED_INDEX, 1500); // smooth acceleration bay-bay
			
			//servo.setPosition(0, 90);
			//servo.setEngaged(0, true);
			servo.setSpeedRampingOn(0, true);
			servo.setSpeedRampingOn(1, true);
		} catch (PhidgetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * check the bounds of a double
	 * @param d
	 * @param max
	 * @param min
	 * @return d if it lies between max and min, or whichever it is closer to.
	 * @throws NumberFormatException if max < min
	 */
	private static final double constrain(double d, double max, double min) throws NumberFormatException
	{
		if (max < min)
		{
			throw new NumberFormatException("max (" + max + ") is less than min (" + min + ")!!!");
		}
		
		if (d > max)
		{
			return max;
		} else {
			if (d < min) {
				return min;
			} else {
				return d;
			}
		}
	}

}
