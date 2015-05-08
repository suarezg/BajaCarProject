package ca.umanitoba.me.carcontroller;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Interpret a steering profile to determine how to steer the car.
 * @author Paul White
 * @version 0.1
 *
 */
public class ControlProfile 
{
	private long stepSizeMillis; // time between samples
	private boolean isFileRadians;
	//private AngleSpeed[] anglesSpeeds;

//	private SteeringController steeringController;
//	private SpeedController speedController;

	// raw data
	private Float[] timesRaw = new Float[0];
	private Float[] anglesRaw = new Float[0];
	private Float[] speedsRaw = new Float[0];

	// resampled data
	private Float[] angles = new Float[0];
	public Float[] getAngles() { return angles.clone(); }
	
	private Float[] speeds = new Float[0];
	public Float[] getSpeeds() { return speeds.clone(); }

	/**
	 * Default Constructor
	 */
	@SuppressWarnings(value = { "unused" }) 
	private ControlProfile()
	{

	}

	//	/**
	//	 * Construct a using pre-defined profile
	//	 * @param steeringProfileAngles a list of angles to set the front wheels at
	//	 * @param stepSizeMillis number of milliseconds between each sample. (Ensure that this does not exceed the servo's slew rate)
	//	 */
	//	public ProfileInterpreter(float[] steeringProfileAngles, long stepSizeMillis)
	//	{
	//		speedController = null; // initialize controller
	//		steeringController = null;
	//		this.angles = steeringProfileAngles;
	//		this.stepSizeMillis = stepSizeMillis;
	//	}

	/**
	 * Convenience constructor to load the profile from a file
	 * @param steeringProfileFile
	 * @param isRadians is the steering profile in radians or degrees
	 * @throws FileNotFoundException if <code>steeringProfileFile</code> could not be opened
	 */
	public ControlProfile(File steeringProfileFile, long stepSizeMillis, boolean isRadians) throws FileNotFoundException
	{
//		speedController = null; // initialize controller
//		steeringController = null;

		this.stepSizeMillis = stepSizeMillis;
		this.isFileRadians = isRadians;
		
		if (!setProfile(steeringProfileFile, isFileRadians))
		{
			throw new FileNotFoundException("File " + steeringProfileFile.getAbsolutePath() + " was opened, but is not formatted correctly.");
		}
	}
	
	/**
	 * Load the profile from a path
	 * @param steeringProfileFilePath
	 * @param stepSizeMillis
	 * @param isRadians
	 * @throws FileNotFoundException if the file pointed to by <code>steeringProfileFilePath</code> could not be opened
	 */
	public ControlProfile(String steeringProfileFilePath, long stepSizeMillis, boolean isRadians) throws FileNotFoundException
	{
		this.stepSizeMillis = stepSizeMillis;
		this.isFileRadians = isRadians;
		
		File profileFile = new File(steeringProfileFilePath);
		
		setProfile(profileFile, isFileRadians);
	}



	public void print()
	{
		System.out.println("Raw data:");
		for (int indexRaw = 0; indexRaw < timesRaw.length; indexRaw++) 
		{
			System.out.println(timesRaw[indexRaw] + "\t" + anglesRaw[indexRaw]);
		}
		
		System.out.println("");
		System.out.println("new interval: " + stepSizeMillis + "ms");
		for (int indexParsed = 0; indexParsed < angles.length; indexParsed ++)
		{
			System.out.println(stepSizeMillis * indexParsed / 1000.0 + "\t" + angles[indexParsed]);
		}
	}
	
	/**
	 * Load the steering profile from a file, and re-sample it according to <code>stepSizeMillis</code>
	 * @param profileFile
	 * @param isRadians
	 * @return <code>true</code> if the file was parsed successfully, and <code>false</code> if the
	 * file was not encoded properly.
	 * @throws FileNotFoundException if <code>profileFile</code> could not be opened
	 */
	public boolean setProfile(File profileFile, boolean isRadians) throws FileNotFoundException
	{
		boolean encodingOK = true;

		List<List<Float>> profileData = readCSVFile(profileFile, isRadians);
		try 
		{
			timesRaw = profileData.get(0).toArray(new Float[0]);
			anglesRaw = profileData.get(1).toArray(new Float[0]);

			if (profileData.size() > 2)
			{
				speedsRaw = profileData.get(2).toArray(new Float[0]);
			} else 
			{
				speedsRaw = new Float[0];
			}
			
			angles = setInterval(stepSizeMillis, timesRaw, anglesRaw);
			if (speedsRaw.length > 0)
			{
				speeds = setInterval(stepSizeMillis, timesRaw, speedsRaw);
			}
			
		} catch (ArrayIndexOutOfBoundsException e) 
		{
			encodingOK = false;
		}

		return encodingOK;
	}

	/**
	 * Read a CSV file composed of floating point numbers.
	 * @param profileFile the <code>File</code> object that represents the file we wish to open
	 * @param isFileRadians Is the second column a list of angles in radians? <code>true</code> -> convert to degrees.
	 * @return A 2-D list of data where each <code>List&ltFloat&gt</code> is a column in the CSV file.
	 * @throws FileNotFoundException if <code>profileFile</code> cannot be read or opened.
	 */
	private static List<List<Float>> readCSVFile(File profileFile, boolean isFileRadians) throws FileNotFoundException
	{
		List<List<Float>> dataLists = new ArrayList<>();

		try {
			FileReader reader = new FileReader(profileFile);
			BufferedReader bufferedReader = new BufferedReader(reader);
			String line = bufferedReader.readLine();
			while (null != line) // rows
			{
				// parse the ordered pair
				try 
				{
					String[] data = line.split(",");

					// create dataLists if it does not exist
					while (dataLists.size() < data.length) 
					{
						dataLists.add(new ArrayList<Float>());
					}

					for (int columnIndex = 0; columnIndex < data.length; columnIndex++) // columns
					{
						float datumRaw = Float.parseFloat(data[columnIndex]);
						float datum;
						
						if (isFileRadians && (columnIndex == 1)) // convert second column to degrees
						{
							datum = (180.0f * datumRaw / 3.14159f);
						} else {
							datum = datumRaw;
						}
						
						
						List<Float> columnList = dataLists.get(columnIndex);
						
						columnList.add(datum);
					}
				} catch (Exception e) 
				{
					// some error in parsing the data
					System.err.println(e);
					break;
				}

				// next line
				line = bufferedReader.readLine();
			}

			bufferedReader.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return dataLists;
	}

//	/**
//	 * Load the data in <code>profileFile</code> into <code>times</code> and <code>anglesRadians</code>.
//	 * @param times The time points will be stored in this <code>list</code>
//	 * @param anglesRadians The angles will be stored in this <code>list</code>
//	 * @param profileFile point to the source file. Note that this should be a file of type ".str" as produced by the MATLAB model.
//	 * This means that it must have two columns of data. The first column contains timestamps and the second column
//	 * contains the desired steering angle for that timestamp.
//	 * @return <code>true</code> if the data in <code>profileFile</code> was successfully read. Returns <code>false</code>
//	 * if the file was not correctly encoded as an STR file.
//	 */
//	private static boolean readStrFile(List<Float> times, List<Float> anglesRadians, File profileFile)
//	{
//		boolean encodingOk = true;
//
//
//		try {
//			FileReader reader = new FileReader(profileFile);
//			BufferedReader bufferedReader = new BufferedReader(reader);
//			String line = bufferedReader.readLine();
//			while (null != line)
//			{
//				// parse the ordered pair
//				try {
//					String[] data = line.split(",");
//					times.add(Float.parseFloat(data[0]));
//					anglesRadians.add(Float.parseFloat(data[1]));
//				} catch (Exception e) 
//				{
//					// some error in parsing the data
//					System.err.println(e);
//					encodingOk = false;
//					break;
//				}
//
//				// next line
//				line = bufferedReader.readLine();
//			}
//
//			bufferedReader.close();
//
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		return encodingOk;
//	}


	/**
	 * Parse some data and re-sample it to have a constant time interval of <code>intervalMillis</code>
	 * @param intervalMillis the new time interval
	 * @param times the original time series
	 * @param data the original series of data
	 * @return a new series of data that matches a constant time series with a spacing of <code>intervalMillis</code>.
	 * There should be <code>intervalMillis</code>/<code>elapsedTime</code> elements in the resulting array.
	 */
	private static Float[] setInterval(long intervalMillis, Float[] times, Float[] data)
	{
		float elapsedTime;

		// calculate elapsed time
		float startTime = times[0];
		float endTime = times[times.length - 1];
		elapsedTime = endTime - startTime;

		// total number of indices in resultant array
		float intervalSecs = ((float)intervalMillis) / (1000.0f);
		int resampledDataLength = (int) (elapsedTime / intervalSecs) + 1;

		int dataIndex = 1;

		// initialize variables that will be used in the loop
		float averageData;
		float numberOfSamples;
		float currentTimeSecs;
		//			float lastTimeSecs = times.get(0);

		// initialize return buffer
		Float[] resampledData = new Float[resampledDataLength];
		resampledData[0] = data[0];

		// traverse resampledData array and populate it with the averages of the raw data
		for (int currentIndex = 0; currentIndex < resampledDataLength; currentIndex++)
		{

			numberOfSamples = 1;
			averageData = data[dataIndex];
			currentTimeSecs = (currentIndex + 0.5f) * intervalMillis / 1000.0f; // what is the time in millis at the current index in the result array?
			//				lastTimeSecs = (currentIndex - 0.5f) * intervalMillis / 1000.0f;
			//				lastTimeSecs = times.get(dataIndex);

			// calculate average angle/speed 
			if (times[dataIndex] < currentTimeSecs)
			{
				dataIndex ++;

				while (dataIndex < times.length && (times[dataIndex] < currentTimeSecs)) 
				{								
					//float weight = (times.get(dataIndex) - times.get(dataIndex - 1)) / (currentTimeSecs - lastTimeSecs); // use to smoothen out slopes; does not work yet
					averageData += data[dataIndex];
					numberOfSamples++;

					dataIndex ++;
				}
			}

			averageData /= numberOfSamples;
			//System.out.println(numberOfSamples + " samples for t < " + currentTimeSecs);
			resampledData[currentIndex] = averageData;
		}

		return resampledData;
	}

//	/**
//	 * Parse the angle data and re-sample it to have a constant time interval of <code>intervalMillis</code>
//	 * @param intervalMillis the new time interval
//	 * @param times the original time series
//	 * @param angles the original series of angles
//	 * @param speed the constant speed
//	 * @return a new series of angles/speeds that matches a constant time series with a spacing of <code>intervalMillis</code>.
//	 * There should be <code>intervalMillis</code>/<code>elapsedTime</code> elements in the resulting array.
//	 */
//	private static AngleSpeed[] setInterval(long intervalMillis, List<Float> times, List<Float> angles, float speed)
//	{
//		float elapsedTime;
//
//		// calculate elapsed time
//		float startTime = times.get(0);
//		float endTime = times.get(times.size() - 1);
//		elapsedTime = endTime - startTime;
//
//		// total number of indices in resultant array
//		float intervalSecs = ((float)intervalMillis) / (1000.0f);
//		int resampledDataLength = (int) (elapsedTime / intervalSecs) + 1;
//
//		int dataIndex = 1;
//
//		// initialize variables that will be used in the loop
//		float averageAngle;
//		float averageSpeed;
//		float numberOfSamples;
//		float currentTimeSecs;
//		//		float lastTimeSecs = times.get(0);
//
//		// initialize return buffer
//		AngleSpeed[] resampledData = new AngleSpeed[resampledDataLength];
//		resampledData[0] = new AngleSpeed(angles.get(0), speed);
//
//		// traverse resampledData array and populate it with the averages of the raw data
//		for (int currentIndex = 0; currentIndex < resampledDataLength; currentIndex++)
//		{
//
//			numberOfSamples = 1;
//			averageAngle = angles.get(dataIndex);
//			//averageSpeed = speeds.get(dataIndex);
//			currentTimeSecs = (currentIndex + 0.5f) * intervalMillis / 1000.0f; // what is the time in millis at the current index in the result array?
//			//			lastTimeSecs = (currentIndex - 0.5f) * intervalMillis / 1000.0f;
//			//			lastTimeSecs = times.get(dataIndex);
//
//			// calculate average angle/speed 
//			if (times.get(dataIndex) < currentTimeSecs)
//			{
//				dataIndex ++;
//
//				while (dataIndex < times.size() && (times.get(dataIndex) < currentTimeSecs)) 
//				{								
//					//float weight = (times.get(dataIndex) - times.get(dataIndex - 1)) / (currentTimeSecs - lastTimeSecs); // use to smoothen out slopes; does not work yet
//					averageAngle += angles.get(dataIndex);
//					//					averageSpeed += speeds.get(dataIndex);
//					numberOfSamples++;
//
//					dataIndex ++;
//				}
//			}
//
//			averageAngle /= numberOfSamples;
//			//			averageSpeed /= numberOfSamples;
//			averageSpeed = speed;
//			//System.out.println(numberOfSamples + " samples for t < " + currentTimeSecs);
//			resampledData[currentIndex] = new AngleSpeed(averageAngle, averageSpeed);
//		}
//
//		return resampledData;
//	}

//	/**
//	 * Parse the angle data and re-sample it to have a constant time interval of <code>intervalMillis</code>
//	 * @param intervalMillis the new time interval
//	 * @param times the original time series
//	 * @param angles the original series of angles
//	 * @param speed the constant speed
//	 * @return a new series of angles that matches a constant time series with a spacing of <code>intervalMillis</code>.
//	 * There should be <code>intervalMillis</code>/<code>elapsedTime</code> elements in the resulting array.
//	 */
//	public static float[] setInterval(long intervalMillis, List<Float> times, List<Float> angles)
//	{
//		float elapsedTime;
//
//		// calculate elapsed time
//		float startTime = times.get(0);
//		float endTime = times.get(times.size() - 1);
//		elapsedTime = endTime - startTime;
//
//		// total number of indices in resultant array
//		float intervalSecs = ((float)intervalMillis) / (1000.0f);
//		int resampledDataLength = (int) (elapsedTime / intervalSecs) + 1;
//
//		int dataIndex = 1;
//
//		// initialize variables that will be used in the loop
//		float averageAngle;
//		float numberOfSamples;
//		float currentTimeSecs;
//		//		float lastTimeSecs = times.get(0);
//
//		// initialize return buffer
//		float[] resampledData = new float[resampledDataLength];
//		resampledData[0] = angles.get(0);
//
//		// traverse resampledData array and populate it with the averages of the raw data
//		for (int currentIndex = 0; currentIndex < resampledDataLength; currentIndex++)
//		{
//
//			numberOfSamples = 1;
//			averageAngle = angles.get(dataIndex);
//			//averageSpeed = speeds.get(dataIndex);
//			currentTimeSecs = (currentIndex + 0.5f) * intervalMillis / 1000.0f; // what is the time in millis at the current index in the result array?
//			//			lastTimeSecs = (currentIndex - 0.5f) * intervalMillis / 1000.0f;
//			//			lastTimeSecs = times.get(dataIndex);
//
//			// calculate average angle/speed 
//			if (times.get(dataIndex) < currentTimeSecs)
//			{
//				dataIndex ++;
//
//				while (dataIndex < times.size() && (times.get(dataIndex) < currentTimeSecs)) 
//				{								
//					//float weight = (times.get(dataIndex) - times.get(dataIndex - 1)) / (currentTimeSecs - lastTimeSecs); // use to smoothen out slopes; does not work yet
//					averageAngle += angles.get(dataIndex);
//					//					averageSpeed += speeds.get(dataIndex);
//					numberOfSamples++;
//
//					dataIndex ++;
//				}
//			}
//
//			averageAngle /= numberOfSamples;
//			//System.out.println(numberOfSamples + " samples for t < " + currentTimeSecs);
//			resampledData[currentIndex] = averageAngle;
//		}
//
//		return resampledData;
//	}


}
