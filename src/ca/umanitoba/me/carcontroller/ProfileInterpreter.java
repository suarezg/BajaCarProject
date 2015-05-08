package ca.umanitoba.me.carcontroller;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Timer;
import java.util.TimerTask;





//import ca.umanitoba.me.carcontroller.impl.DummySpeedSteeringController;
import ca.umanitoba.me.carcontroller.impl.ServoManager;
import ca.umanitoba.me.sensors.SensorListener;
import ca.umanitoba.me.sensors.SensorManager;
import ca.umanitoba.me.sensors.impl.BajaSensor;
import ca.umanitoba.me.sensors.impl.DummySensorManager;
import ca.umanitoba.me.ui.BajaUserInterface;
import ca.umanitoba.me.ui.BajaUserInterface.UIEvent;

/**
 * Interpret a steering profile to determine how to steer the car. Sends messages to a 
 * <code>SteeringController</code> and a <code>SpeedController</code>
 * @author Paul White
 * @version 0.1
 *
 */
public class ProfileInterpreter implements SensorListener
{
	private static final String sensorString = BajaSensor.LongitudinalSpeed.toString(); //"sin";
	
	public static void main(String[] args) throws FileNotFoundException
	{
		File profileFile = new File("steeringProfile.str");
		
		SensorManager dummy = DummySensorManager.getTheInstance();
		dummy.startSensors();
		
		ProfileInterpreter interpreter = new ProfileInterpreter(profileFile, 75, 0, dummy, null);
		
		interpreter.execute();
	}
	
	
	private SteeringController steeringController;
	private SpeedController speedController;
	private ControlProfile profile;
	private long stepSizeMillis;
	
	private double startSpeed;
	private final double startSpeedThresh = 10; // percent
	
	private boolean isControlling;
	private boolean waitingToStart;
	
	private BajaUserInterface ui;
	
	private SensorManager sensors;
	
	/**
	 * Construct a new instance of <code>ProfileInterpreter</code>. This constructor also initializes instances of 
	 * <code>SteeringController</code> and a <code>SpeedController</code> to issue steering/speed commands to.
	 * @param steeringProfileFile
	 * @param stepSizeMillis
	 * @param startSpeed
	 * @param sensors
	 * @throws FileNotFoundException
	 */
	public ProfileInterpreter(File steeringProfileFile, long stepSizeMillis, double startSpeed, SensorManager sensors, BajaUserInterface ui) throws FileNotFoundException
	{
		// use dummys for testing
//		DummySpeedSteeringController controller = new DummySpeedSteeringController();
//		speedController = controller; // initialize controller
//		steeringController = controller;
		
		// fer real
		speedController = ServoManager.getSpeedController();
		steeringController = ServoManager.getSteeringController();
		
		this.stepSizeMillis = stepSizeMillis;
		this.startSpeed = startSpeed;
		
		this.isControlling = false;
		this.waitingToStart = false;
		
		this.ui = ui;
		
		profile = new ControlProfile(steeringProfileFile, stepSizeMillis, true);
		
		this.sensors = sensors;

	}
	
	/**
	 * Execute the stored steering profile.
	 */
	public void execute()
	{
		System.out.println("Execute!");
		this.waitingToStart = true;		
		
		if (this.startSpeed != 0) 
		{
			speedController.turnOn();
			if (this.sensors != null) 
			{
				System.out.println("Add Listener: " + sensorString);
				this.sensors.addListener(this, sensorString);
				//this.sensors.addListener(this, "sin");
			}	
			steeringController.setSteeringAngle(0);
			speedController.setCarSpeed(0);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} // reset ESC
			speedController.setCarSpeed((float) startSpeed);
			System.out.println("set car speed: " + startSpeed + " m/s");
			
			// wait until it is at the desired speed, then start steering
		}
		
		else 
		{
			startSteering();
			// just go for it mang
		}
		
	}
	
	private void startSteering()
	{
		final Float[] steeringAnglesToExecute = profile.getAngles();
		final Float[] speedsToExecute = profile.getSpeeds();
		final Timer nextSteeringAngleTimer = new Timer("SteeringTimer");// schedule a TimerTask to periodically loop through the list of angles

		TimerTask steeringTask = new TimerTask() 
		{
			int index = 0;
			@Override
			public void run() 
			{
				if (isControlling && (index < steeringAnglesToExecute.length))
				{
					steeringController.setSteeringAngle(steeringAnglesToExecute[index]);
					if (steeringAnglesToExecute.length == speedsToExecute.length) 
					{
						speedController.setCarSpeed(speedsToExecute[index]);
					}					
					index ++;
				} else 
				{
					halt();	
					this.cancel();				
				}				
			}
		};

		this.isControlling = true;
		nextSteeringAngleTimer.schedule(steeringTask, 0, stepSizeMillis);
	}
	
	/**
	 * Stops the stored steering profile
	 */
	public void halt()
	{
		System.out.println("Halt profile");

		this.waitingToStart = false;
		speedController.setCarSpeed(0);
		this.sensors.removeListener(this, sensorString);
		
		speedController.turnOff();
		
		if (isControlling) 
		{
			isControlling = false;
		} 	
		System.out.println("Profile finished.");
		if (null != ui)
		{
			ui.notifyUI(UIEvent.DID_FINISH_PROFILE);
		}
	}

	@Override
	public void sensorChanged(double value, String sensorType) 
	{		
		if (sensorType.equals(sensorString))
		//if (sensorType.equals("sin"))
		{
			//System.out.println("Speed: " + value);
			if (waitingToStart  && (value >= (startSpeed * (1.0 - startSpeedThresh/100.0))))
			{
				
				waitingToStart = false;
				this.sensors.removeListener(this, sensorString);				
				startSteering();
			}
		} else {
			System.out.println("sensor " + sensorType + ": " + value);
		}
	}
		
	

}
