package ca.umanitoba.me.sensors;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import ca.umanitoba.me.car.filemanager.BajaFileManager;

public class SensorLogger
{
	public static final String OUTPUT_VOLUME = "BAJA_DISK";
	
	private SensorManager sensors;
	private long loggingInterval;
	private long startTime;
	private FileOutputStream mFileOutputStream;
	private File currentFile;
	private Timer logTimer;
		
	/* Hide default constructor */
	@SuppressWarnings("unused")
	private SensorLogger()
	{	}
	
	/**
	 * Construct a sensor logger that can read sensor values and write them to a file.
	 * Sample at default rate.
	 * @param sensors
	 */
	public SensorLogger(SensorManager sensors)
	{
		this(sensors, 50);
	}
	
	/**
	 * Construct a sensor logger that can read sensor values and write them to a file.
	 * @param sensors
	 * @param intervalMS default sampling interval to use
	 * @throws FileNotFoundException 
	 */
	public SensorLogger(SensorManager sensors, long intervalMS)
	{
		this.loggingInterval = intervalMS;
		this.sensors = sensors;
	}
	
	private void initializeOutputFile() throws FileNotFoundException
	{
//		long startTime = System.currentTimeMillis();
		
		currentFile = getNewOutputFile();
//		long getOutputFileTime = System.currentTimeMillis();
		
		mFileOutputStream = new FileOutputStream(currentFile);	
//		long createOutputStreamTime = System.currentTimeMillis();	
		
		//System.out.println("create new output file: " + currentFile.getAbsolutePath());
//		System.out.println("create output stream: " + (createOutputStreamTime - getOutputFileTime));
	}
	
	/**
	 * Commence logging to a new file at a custom sampling frequency
	 * @param samplingInterval
	 * @throws FileNotFoundException output file could not be written to
	 */
	public boolean start(long samplingInterval) throws FileNotFoundException
	{
		boolean allOk = true;
		
		startTime = System.currentTimeMillis();
		logTimer = new Timer("Log Timer Thread");		
		
		initializeOutputFile();
			
		// prime the new file
		if (null != mFileOutputStream) 
		{
			// build a header with all the sensor names
			String header = "Time (seconds),";
			String[] sensorStrings = sensors.getSensorNames();
			for (String currentSensor : sensorStrings) 
			{
				header += currentSensor + ",";
			}			
			header += '\n';
			
			try {
				mFileOutputStream.write(header.getBytes());
				mFileOutputStream.flush();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				allOk = false;
				return allOk;
			}		
		} else {
			allOk = false;
			return allOk;
		}
		
		// schedule a TimerTask to write data
		TimerTask writeDataTask = new TimerTask() {
			
			@Override
			public void run() 
			{
				
				double[] data = sensors.getSensorData();
				long currentTime = System.currentTimeMillis() - startTime;
				String dataLineString = Double.toString(currentTime / 1000.0) + ",";
				
				for (double d : data) 
				{
					dataLineString += d + ",";
				}
				dataLineString += '\n';
				
				try {
					mFileOutputStream.write(dataLineString.getBytes());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};
		
		logTimer.scheduleAtFixedRate(writeDataTask, 0, samplingInterval);
		
		return allOk;
	}
	
	/** 
	 * Commence logging to a new file at the default sampling frequency
	 * @throws FileNotFoundException output file could not be written to
	 */
	public void start() throws FileNotFoundException
	{
		start(loggingInterval);
	}
	
	/**
	 * stop logging
	 */
	public boolean stop()
	{
		boolean allOk = true;
		
		if (null != logTimer)
		{
			logTimer.cancel();
			logTimer = null;
		}
		
		
		if (null != mFileOutputStream) 
		{
			try 
			{
				mFileOutputStream.flush();
				mFileOutputStream.close();				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				allOk = false;
			}			
		}
		
		return allOk;		
	}
	
	/** 
	 * Set a custom filename after the log is done
	 * @param filename new file name
	 * @return <code>true</code> if the file was successfully renamed, <code>false</code> 
	 * if it failed or if the last file = null.
	 */
	public boolean renameCurrentFile(String filename)
	{
		if (null != currentFile)
		{
			try {
				return currentFile.renameTo(getNewOutputFile(filename));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		else {
			return false;
		}
	}
	
	/**
	 * @return A new File object to write to, or <code>null</code> if the volume 
	 * could not be found
	 * @throws FileNotFoundException 
	 */
	private File getNewOutputFile() throws FileNotFoundException
	{
		Date fileCreationDate = new Date();
		DateFormat filenameFormatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String name = filenameFormatter.format(fileCreationDate);
		return getNewOutputFile(name);
	}


	/**
	 * Generate a file to output to in the Output volume
	 * @param name what to name the file
	 * @return A new File object to write to, or <code>null</code> if the volume 
	 * could not be found
	 * @throws FileNotFoundException 
	 */
	private File getNewOutputFile(String name) throws FileNotFoundException
	{	
		String fileNameString = "data-" + name + ".csv";	
		return new File(BajaFileManager.getOutputFileDirectory(), fileNameString);
	}	
	
}
