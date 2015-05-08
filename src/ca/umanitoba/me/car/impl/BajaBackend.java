package ca.umanitoba.me.car.impl;

import java.io.File;
import java.io.FileNotFoundException;

import ca.umanitoba.me.car.Backend;
import ca.umanitoba.me.car.filemanager.BajaFileManager;
import ca.umanitoba.me.carcontroller.ProfileInterpreter;
import ca.umanitoba.me.sensors.SensorLogger;
import ca.umanitoba.me.sensors.SensorManager;
import ca.umanitoba.me.sensors.impl.BajaSensorManager;
import ca.umanitoba.me.sensors.impl.DigitalOutput;
import ca.umanitoba.me.ui.BajaUserInterface;
import ca.umanitoba.me.ui.BajaUserInterface.UIEvent;

/**
 * An implementation of {@link ca.umanitoba.me.car.Backend Backend} for the 1/5 scale electric Baja vehicle.
 * @author Paul White
 * @version 0.1
 *
 */
public class BajaBackend implements Backend {
	
	private static final int PROFILE_INTERPRETER_INTERVAL = 200; // ms
	private static final int LOGGING_INTERVAL = 70; // ms
	
	SensorManager sensors;
	SensorLogger logger;
	BajaUserInterface ui;
	ProfileInterpreter profileInterpreter;
	
	
	public BajaBackend()
	{
		sensors = BajaSensorManager.getTheInstance();
		//sensors = DummySensorManager.getTheInstance();
		logger = new SensorLogger(sensors);
		profileInterpreter = null;
	}
	
	@Override
	public double[] getSensorData() // thread safe
	{
		return sensors.getSensorData();
	}

	@Override
	public String[] getSensorNames() // thread safe
	{
		return sensors.getSensorNames();
	}

	@Override
	public synchronized void startProfile() 
	{
		if (null != profileInterpreter) {
			// message to steering controller...
			profileInterpreter.execute(); // spawns a new thread that we can stop at any time
			
			// automatically start logging
			//startLog();
		} else {
			ui.notifyUI(UIEvent.DID_FINISH_PROFILE);
		}
	}

	@Override
	public synchronized void stopProfile()
	{
		if (null != profileInterpreter) 
		{
			// message to steering controller...
			profileInterpreter.halt(); // stops thread that profileInterpreter spawned
		}
	}

	@Override
	public synchronized boolean setProfileFilePath(String profileFilePath) 
	{
		return setProfileFilePath(profileFilePath, 0);
	}
	
	@Override
	public synchronized boolean setProfileFilePath(String profileFilePath, double startSpeed) 
	{
		try {
			profileInterpreter = new ProfileInterpreter(new File(profileFilePath), PROFILE_INTERPRETER_INTERVAL, startSpeed, BajaSensorManager.getTheInstance(), ui);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public synchronized void setAutomaticDrivingMode(boolean automatic) 
	{
		DigitalOutput.setAutomaticControl(automatic);
	}

	@Override
	public synchronized void startSensors() 
	{
		sensors.startSensors();
	}

	@Override
	public synchronized void stopSensors() 
	{
		sensors.closeSensors();
	}

	@Override
	public synchronized void startLog() 
	{
		startLog(LOGGING_INTERVAL);
	}

	@Override
	public synchronized void startLog(long sampleInterval) 
	{
		if (null != logger)
		{
			try {
				if (logger.start(sampleInterval)) 
				{
					ui.notifyUI(UIEvent.STARTED_LOG);
				} else 
				{
					ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
				}
			} catch (FileNotFoundException e) {
				ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
			}
		} else {
			ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
		}
	}

	@Override
	public synchronized void stopLog() 
	{
		if (null != logger)
		{
			if (logger.stop())
			{
				ui.notifyUI(UIEvent.FINISHED_LOG);
			}
		} else {
			ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
		} 
	}

	@Override
	public synchronized void setUserInterface(BajaUserInterface ui) 
	{
		this.ui = ui;
		
		if ((null == logger) || (!BajaFileManager.checkOutputVolume())) 
		{
			this.ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
		}
	}

	@Override
	public synchronized boolean renameLastLogFile(String newFileName) 
	{
		if (null != logger) {
			return logger.renameCurrentFile(newFileName);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public boolean checkOutputVolume() // thread safe
	{
		return BajaFileManager.checkOutputVolume();
	}

	@Override
	public File[] getProfilePaths()  // thread safe
	{
		File[] knownProFiles = new File[0];
		try {
			knownProFiles = BajaFileManager.getProfileFiles();
		} catch (FileNotFoundException e) 
		{
			ui.notifyUI(UIEvent.COULD_NOT_FIND_OUTPUT_VOLUME);
		}
		
		return knownProFiles;
	}
}
