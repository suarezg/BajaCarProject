package ca.umanitoba.me.ui.cmd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import ca.umanitoba.me.car.Backend;
import ca.umanitoba.me.ui.BajaUserInterface;

public class CommandUI implements BajaUserInterface {

	private Thread uiThread;
	private Backend mBackend;
	
	public CommandUI()
	{
		this(null);
	}
	
	
	public CommandUI(Backend backend)
	{
		mBackend = backend;
	}
	
	public enum Commands
	{
		startSensors,
		startLogging,
		stopSensors,
		stopLogging,
		setProfile,
		startProfile,
		stopProfile,
		quit
	}
	
	@Override
	public void startUI()
	{
		uiThread = new Thread(new Runnable() {
			
			@Override
			public void run() 
			{
				System.out.println("Welcome!");
				printCommandList();
				// handle user interface (copied from Shawn)
				BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
				
				boolean keepGoing = true;
				while (keepGoing)
				{
					String line;
					try {
						line = input.readLine();			
						String[] splitLine = line.split(" ");
						String commandString = splitLine[0];
						
						if (Commands.startSensors.toString().equals(commandString))
						{
							System.out.println("start sensors...");
							mBackend.startSensors();
						} else if (Commands.startLogging.toString().equals(commandString))
						{
							if (splitLine.length > 1) 
							{
								try {
									long interval = Long.parseLong(splitLine[1]);
									System.out.println("start logging every " + interval + "ms");
									mBackend.startLog(interval);
								} catch (NumberFormatException e) {
									System.out.println(splitLine[1] + " should be an integer");
								}
								
							} else {
								System.out.println("start logging...");
								mBackend.startLog();
							}							
						} else if (Commands.stopSensors.toString().equals(commandString))
						{							
							mBackend.stopSensors();
							System.out.println("stopped sensors");
						} else if (Commands.stopLogging.toString().equals(commandString))
						{
							mBackend.stopLog();
							System.out.println("stopped logging");
						} else if (Commands.startProfile.toString().equals(commandString))
						{
							System.out.println("start profile...");
							mBackend.startProfile();
						} else if (Commands.stopProfile.toString().equals(commandString))
						{
							mBackend.stopProfile();
							System.out.println("stopped profile");
						} else if (Commands.quit.toString().equals(commandString))
						{
							mBackend.stopProfile();
							mBackend.stopLog();
							mBackend.stopSensors();
							keepGoing = false;
						} else if (Commands.setProfile.toString().equals(commandString))
						{
							String filePath = "steeringProfile.str";
							if (splitLine.length > 1) 
							{
								filePath = splitLine[1];
							}		
							
							mBackend.setProfileFilePath(filePath);
						} else {
							printCommandList();
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}, "UI Thread");
		uiThread.start();
	}
	
	private static void printCommandList()
	{
		System.out.println("Command list:");
		for (Commands command : Commands.values()) 
		{
			System.out.println(command.toString());
		}
	}
	
	@Override
	public void notifyUI(UIEvent event) 
	{
		switch (event) {
		case DID_FINISH_PROFILE:
			System.out.println("Finished running the profile!");
			break;

		default:
			System.out.println(event);
			break;
		}
	}


	@Override
	public void setBackend(Backend be) 
	{
		this.mBackend = be;
	}

}
