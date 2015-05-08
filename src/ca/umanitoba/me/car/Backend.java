package ca.umanitoba.me.car;

import java.io.File;

import ca.umanitoba.me.ui.BajaUserInterface;

/**
 * This interface dictates the functions that a User Interface should expect to see exposed to it.
 * 
 * See {@link ca.umanitoba.me.carcontroller#BajaBackend} for a concrete implementation.
 * @author Paul
 * @version 0.1
 */
public interface Backend
{	
	/**
	 * attach a User Interface to this backend
	 */
	public void setUserInterface(BajaUserInterface ui);
	
	/**
	 * Initialize sensors and begin taking readings. (Use {@link #startLog()} 
	 * or {@link #startLog(long)} to begin logging data to external storage)
	 */
	public void startSensors();
	
	/**
	 * release sensors and stop getting readings. (Use {@link #stopLog()} to
	 * just stop logging to storage).
	 */
	public void stopSensors();
	
	/**
	 * Start logging data at the default sampling interval
	 */
	public void startLog();
	
	/**
	 * Start logging data at a custom sampling interval
	 * @param sampleInterval sampling interval to use
	 */
	public void startLog(long sampleInterval);

	/**
	 * Stop logging data
	 */
	public void stopLog();
	
	/**
	 * Called by UI to request sensor data for any attached sensors. Polls the 
	 * <code>SensorManager</code> for its current sensor data.
	 * @return an array of <code>double</code>s where each index corresponds to a data value we may want to view.
	 * This data should correspond to the sensor names returned by {@link #getSensorNames()}.
	 */
	public double[] getSensorData();
	
	/**
	 * Called by the UI to request the names of all supported sensors. This allows SensorManager to
	 * be extended without needing to change the UI.
	 * @return an array of <code>String</code>s where the <code>String</code> at each index contains the name
	 * of a sensor. These names should correspond to the data in the array returned by {@link #getSensorData()}.
	 */
	public String[] getSensorNames();
	
	/**
	 * Begin executing the set profile. If a profile is already executing, start again.
	 */
	public void startProfile();
	
	/**
	 * Stop executing the currently running profile. (if no profile is running, do nothing)
	 */
	public void stopProfile();
	
	/**
	 * Call to set the path to the profile that we want to use. Parse if necessary to determine
	 * the servo commands needed.
	 * @param profileFilePath a <code>String</code> representing the complete path to the profile file.
	 * @param startSpeed speed to accelerate to before beginning the turn maneuvers (in m/s)
	 * @return <code>true</code> if a properly formatted profile was found at <code>profileFilePath</code>, false otherwise.
	 */
	public boolean setProfileFilePath(String profileFilePath, double startSpeed);
		
	/**
	 * Call to set the path to the profile that we want to use. Parse if necessary to determine
	 * the servo commands needed.
	 * @param profileFilePath a <code>String</code> representing the complete path to the profile file.
	 * @return <code>true</code> if a properly formatted profile was found at <code>profileFilePath</code>, false otherwise.
	 */
	public boolean setProfileFilePath(String profileFilePath);
	
	/**
	 * @return An array containing paths to all known profiles. This can be displayed to the user
	 * to allow them to select one.
	 */
	public File[] getProfilePaths();
	
	/**
	 * Enable the automatic controller. Note that the profile will not execute until {@link #startProfile()} is called.
	 * @param automatic <code>true</code> if the car should be automatically controlled. Set to <code>false</code> if the
	 * car should be manually controlled.
	 */
	public void setAutomaticDrivingMode(boolean automatic);
	
	/**
	 * Rename the last file to be something different. The user might be presented with a popup that allows
	 * them to specify a filename for their log.
	 * @param newFileName
	 * @return <code>true</code> if the file was successfully renamed, <code>false</code> 
	 * if it failed or if the last file = null.
	 */
	public boolean renameLastLogFile(String newFileName);

	/**
	 * Verify that a volume exists to log data to. Call this function when your UI attaches to this <code>Backend</code>.
	 * This function may allow any data loggers to cache mount points, and lead to faster initialization of the log file.
	 * @return <code>true</code> if the data logging volume is attached.
	 */
	public boolean checkOutputVolume();




}
