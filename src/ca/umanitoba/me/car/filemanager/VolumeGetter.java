package ca.umanitoba.me.car.filemanager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import javax.swing.filechooser.FileSystemView;

/**
 * http://www.rgagnon.com/javadetails/java-0455.html (alternately use http://feeling.sourceforge.net/document/org/eclipse/swt/extension/io/FileSystem.html#FileSystem())
 * http://www.javaworld.com/article/2071275/core-java/when-runtime-exec---won-t.html?page=2
 * @author Paul White (references above)
 */

public class VolumeGetter{ 
	//	
	private static File lastVolumeFile;
	private static String lastVolumeName = "";

	/**
	 * Function actually used allows for debug messages to be printed
	 * @param volumeName
	 * @param debug
	 * @return
	 */
	private static File getFileByVolumeName(String volumeName, boolean debug)
	{		
		File volumeFile = null;

		synchronized (lastVolumeName) // ensure that lastVolumeName is correct
		{
			// check to see if last volume is still valid (to save time)
			if (volumeName.equals(lastVolumeName) && checkFile(lastVolumeFile, true))
			{
				volumeFile = new File(lastVolumeFile.getAbsolutePath());
			} else 
			{		
				String osName = System.getProperty ("os.name");

				if (debug) 
				{
					System.out.println ("OS=" + osName);
				}		

				if (osName.contains("Windows"))
				{
					// OS is windows so use FileSystemView to detect the USB drive
					if (debug) {
						System.out.println (" -OS is windows so use FileSystemView to detect the volume");
					}

					List <File>files = Arrays.asList(File.listRoots());
					for (File f : files) 
					{
						String s1 = FileSystemView.getFileSystemView().getSystemDisplayName(f);
						String detectedVolumeNameString = "PAULISCOOL";
						try {
							detectedVolumeNameString = s1.substring(0, (volumeName.length()));
						} catch (Exception e) {
							System.err.println("VolumeGetter could not parse string: '" + s1 + "'");
						}
						if (detectedVolumeNameString.equals(volumeName))
						{
							if (debug) {
								System.out.println("Found disk: "+ f);
							}
							volumeFile = f;

							break;
						}
					}

				} else if (osName.contains("Linux"))
				{
					// OS is linux so use lsblk command
					if (debug) {
						System.out.println (" -OS is linux so use lsblk command to detect the volume");
					}


					InputStreamReader lsblkReader;
					BufferedReader lsblkBufferedReader;

					Process lsblkProcess;
					try {
						lsblkProcess = Runtime.getRuntime().exec("lsblk -n -o label,mountpoint");
						lsblkReader = new InputStreamReader(lsblkProcess.getInputStream());
						lsblkBufferedReader = new BufferedReader(lsblkReader);

						String line=null;
						while ( (line = lsblkBufferedReader.readLine()) != null)
						{
							if (debug) {
								System.out.println("output>" + line); 
							}
							String[] strings = line.split(" ");
							if (strings.length > 1 && strings[0].equals(volumeName)) 
							{
								volumeFile = new File(strings[1]);

								if (debug)
								{
									System.out.println("Found disk: "+ volumeFile);
								}
							}
						}

					} catch (IOException e) {
						e.printStackTrace();
					}

				}

				else if (osName.contains("Mac OS X"))
				{
					if (debug) {
						System.out.println(" -OS is Mac OS X so try and create the file in '/Volumes/" + volumeName + "' and see if it exists");
					}

					volumeFile = new File("/Volumes/" + volumeName);

					if (volumeFile.exists()) {

						if (debug) {
							System.out.println("Found disk: "+ volumeFile);
						}				
					} else {
						volumeFile = null;
					} 		
				}

				if (null == volumeFile && debug)
				{
					System.out.println("Disk " + volumeName  + " not found.");
				}

				lastVolumeFile = volumeFile;
				lastVolumeName = volumeName;
			}
		}

		return volumeFile;
	}

	/**
	 * Searches for a volume named <code>volumeName</code> and returns a <code>File</code> that 
	 * refers to that volume. Works with Windows, Linux and Mac OS X. (Linux search requires the 
	 * <code>lsblk</code> command to function)
	 * @param volumeName name to search for (e.g. MyBook)
	 * @return a <code>File</code> that refers to <code>volumeName</code>, or <code>null</code> if
	 * the volume could not be found
	 */
	public static File getFileByVolumeName(String volumeName)
	{
		return getFileByVolumeName(volumeName, false);
	}

	public static void main(String args[]) throws FileNotFoundException
	{
		System.out.println("FSTest DEMO!!");
		System.out.println("Usage: java FSTest <Volume Name> <true|false>");
		System.out.println("Set second argument to 'true' for Debug messages!");

		String volString = "Local Disk";
		boolean debug = true;
		if (args.length > 0) 
		{
			volString = args[0];
		}

		if (args.length > 1)
		{
			debug = Boolean.parseBoolean(args[1]);
		}

		getFileByVolumeName(volString, debug);

		File testFile = new File("/media/usb0/BajaData");		

		System.out.println(testFile + " exists? " + checkFile(testFile, debug));

		File testFile2 = getDirectoryOnDrive("BAJA_DISK", "paul/is/very/cool");
		System.out.println(testFile2 + " exists? " + checkFile(testFile2, debug));
	}

	/**
	 * Check to see if a file exists. (Use this instead of <code>File.exists()</code> because Linux systems sometimes mount 
	 * the USB drive in <code>/media/usbX</code> and this folder does not go away when the device is removed)
	 * @return <code>True</code> if <code>file</code> exists. Return <code>False</code> if <code>file</code> is <code>NULL</code>
	 * or does not exist.
	 */
	public static boolean checkFile(File file, boolean debug) // thread safe; uses no variables
	{
		String osName = System.getProperty ("os.name");
		boolean exists = false;

		if (null == file)
		{
			return false;
		}

		if (debug)
		{
			System.out.println("Check file: " + file.getAbsolutePath());
		}

		if (!osName.contains("Linux"))
		{
			exists = file.exists(); // if we are not on linux, no problem
		}

		else // use the linux call 'df'
		{

			InputStreamReader dfReader;
			BufferedReader dfBufferedReader;

			Process dfProcess;
			String filePath = file.getAbsolutePath();
			try {
				String commandString = "df " + filePath;

				if (debug) {
					System.out.println("execute: " + commandString);
				}

				dfProcess = Runtime.getRuntime().exec(commandString); 
				dfReader = new InputStreamReader(dfProcess.getInputStream());
				dfBufferedReader = new BufferedReader(dfReader);

				String line=null;
				while ( (line = dfBufferedReader.readLine()) != null)
				{
					if (debug) {
						System.out.println("output>" + line); 
					}

					// search for an entry that contains '/dev/sdXX' because this will indicate a disk is mounted 
					if (line.contains("/dev/sd"))
					{
						exists = true; // non-null output therefore a file was found
					}
				}

			} catch (IOException e) 
			{
				e.printStackTrace();
			}
		}

		return exists;
	}

	/**
	 * Detect the presence of a volume with the name <code>volumeName</code>, and return a directory pointed to by
	 * <code>directoryPathString</code> that is rooted at <code>volumeName</code>.
	 * @param volumeName The name of the volume to search for 
	 * @param directoryPathString The directory tree to search for (or create if it does not exist)
	 * @return a <code>File</code> object that points to <code>volumeName/directoryPathString</code>
	 * @throws FileNotFoundException if <code>volumeName</code> is not attached, or if the directory tree described by 
	 * <code>directoryPathString</code> could not be created.
	 */
	public static File getDirectoryOnDrive(String volumeName, String directoryPathString) throws FileNotFoundException
	{
		File outputRoot = getFileByVolumeName(volumeName);
		if (null == outputRoot)
		{
			throw new FileNotFoundException("Volume " + volumeName + " not found.");
		}			

		File outputDirectoryFile = new File(outputRoot, directoryPathString);

		if (!outputDirectoryFile.exists()) 
		{
			System.out.println("directory: " + outputDirectoryFile + " does not exist. Creating.");
			outputDirectoryFile.mkdirs();
		}

		if (VolumeGetter.checkFile(outputDirectoryFile, false))
		{
			return outputDirectoryFile;
		}

		else
		{
			throw new FileNotFoundException("Volume " + volumeName + " not found.");
		}
	}
}