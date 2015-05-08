package ca.umanitoba.me.car.filemanager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;

public class BajaFileManager 
{
	public static final String OUTPUT_VOLUME = "BAJA_DISK";
	
	/**
	 * Search the output directory and return a list of output files that are present
	 * @return an array of <code>File</code>, where each <code>File</code> corresponds to a .csv file.
	 * @return an empty array if no .csv files exist.
	 * @throws FileNotFoundException if the output directory could not be opened
	 */
	public static File[] getOutputFiles() throws FileNotFoundException
	{
		File[] exisitingFiles = new File[0];
		File outputDirectoryFile = getOutputFileDirectory();
		
		// only allow STR files
		FilenameFilter filter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				String[] filenameParts = name.split(".");
				return filenameParts[filenameParts.length - 1].equals("csv");
			}
		};
		
		exisitingFiles = outputDirectoryFile.listFiles(filter);
		return exisitingFiles;
	}
	
	/**
	 * Search the profiles directory and return a list of profiles that are present
	 * @return an array of <code>File</code>, where each <code>File</code> corresponds to a .str file.
	 * @return an empty array if no .str files exist.
	 * @throws FileNotFoundException if the profile directory could not be opened
	 */
	public static File[] getProfileFiles() throws FileNotFoundException
	{
		File[] exisitingFiles = new File[0];
		File profileDirectory = getProfileFileDirectory();
		
		// only allow STR files
		FilenameFilter filter = new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				String[] filenameParts = name.split("\\.");
				return filenameParts[filenameParts.length - 1].equals("str");
			}
		};
		
		exisitingFiles = profileDirectory.listFiles(filter);
		return exisitingFiles;
	}
	
	/**
	 * Ensure that a volume is attached for writing data to.
	 * @return
	 */
	public static boolean checkOutputVolume()
	{
		try {
			return VolumeGetter.checkFile(getOutputFileDirectory(), false);
		} catch (Exception e) {
			return false;
		}
		
	}
	
	/**
	 * @return the directory where new output files should be stored.
	 * @throws FileNotFoundException if the correct directory could not be found (data drive is not attached)
	 */
	public static File getOutputFileDirectory() throws FileNotFoundException
	{
		File outputRoot = VolumeGetter.getFileByVolumeName(OUTPUT_VOLUME);
		if (null == outputRoot)
		{
			throw new FileNotFoundException("Volume " + OUTPUT_VOLUME + " not found.");
		}	
		
		
		File outputDirectoryFile = new File(outputRoot, "BajaData");
		
		if (!outputDirectoryFile.exists()) 
		{
			System.out.println("directory: " + outputDirectoryFile + " does not exist. Creating.");
			outputDirectoryFile.mkdir();
		}
		
		if (VolumeGetter.checkFile(outputDirectoryFile, false))
		{
			return outputDirectoryFile;
		}
		
		else
		{
			throw new FileNotFoundException("Volume " + OUTPUT_VOLUME + " not found.");
		}
	}
	
	/**
	 * @return the directory where profile files should be stored.
	 * @throws FileNotFoundException if the correct directory could not be found (data drive is not attached)
	 */
	public static File getProfileFileDirectory() throws FileNotFoundException
	{
		return VolumeGetter.getDirectoryOnDrive(OUTPUT_VOLUME, "profiles");
	}

}
