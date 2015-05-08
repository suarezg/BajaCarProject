package ca.umanitoba.me.ui.web;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletHandler;
import org.msgpack.MessagePack;

import ca.umanitoba.me.car.Backend;
import ca.umanitoba.me.sensors.impl.DigitalOutput;
import ca.umanitoba.me.ui.BajaUserInterface;

/**
 * Allow various WebSocket threads to interface with the {@link ca.umanitoba.me.car.Backend Backend}.
 * A reference to the <code>Backend</code> is not passed because in some cases, we need to check if a given 
 * client actually has permission to send commands to the backend.
 * @author Paul
 *
 */
public class BajaWebServerUI implements BajaUserInterface
{
	private Backend backend;
	private Thread uiThread;

	private Set<BajaUserInterface> pushDelegates;

	private String controllerID = null;

	private BajaWebServerUI()
	{
		@SuppressWarnings(value = { "unused" }) 
		MessagePack messagePack = new MessagePack(); // do this for optimization, when CommandWebSocket tries to use MessagePack, it will lag unless I do this

		System.out.println("Construct new instance of BajaWebServerUI");
		pushDelegates = new HashSet<BajaUserInterface>();
	}

	private static class LazyHolder
	{
		private static final BajaWebServerUI INSTANCE = new BajaWebServerUI();
	}


	public static BajaWebServerUI getTheInstance()
	{
		return LazyHolder.INSTANCE;
	}

	@Override
	public synchronized void notifyUI(UIEvent event) 
	{
		// notify all registered web browsers
		for (BajaUserInterface client : pushDelegates)
		{
			client.notifyUI(event);
		}
	}

	@Override
	public void startUI() 
	{
		if (null == uiThread) 
		{
			uiThread = new Thread(new Runnable() 
			{	
				@Override
				public void run() {
					Server server = new Server(8079);

					// handle requests for static resources
					ResourceHandler resourceHandler = new ResourceHandler();
					resourceHandler.setDirectoriesListed(true);
					resourceHandler.setResourceBase("webpage");
					resourceHandler.setWelcomeFiles(new String[] { "index.html" });

					// handle requests for a websocket
					ServletHandler servletHandler = new UploadServletHandler();
					servletHandler.addServletWithMapping(SensorWebSocketServlet.class, "/sock").setInitOrder(1); // run at startup
					servletHandler.addServletWithMapping(CommandWebSocketServlet.class, "/cmd").setInitOrder(1); // run at startup
					servletHandler.addServletWithMapping(WatchdogWebSocketServlet.class, "/watchdog").setInitOrder(1);

					//handle upload requests
					//					ServletHandler uploadServletHandler = new UploadServletHandler();
					servletHandler.addServletWithMapping(UploadServlet.class, "/upload").setInitOrder(1);

					// group handlers
					HandlerList handlers = new HandlerList();		
					handlers.setHandlers(new Handler[] { resourceHandler, servletHandler });

					// start the server
					server.setHandler(handlers);		
					try {
						server.start();
						System.out.println("Up and running, baby");
						DigitalOutput.setIsReadyLED(true);
						server.join();
						DigitalOutput.setIsReadyLED(false);
					} catch (Exception e) {
						DigitalOutput.setIsReadyLED(false);
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}, "Server Listener");

			uiThread.start();
		} else 
		{
			System.err.println("Could not start UI Thread: A server is already running.");
		}


	}

	@Override
	public synchronized void setBackend(Backend be) 
	{
		this.backend = be;
	}

	//	/**
	//	 * Allow the {@link ca.umanitoba.me.ui.web.SensorWebSocket} to get a reference to the {@link ca.umanitoba.me.car.Backend}
	//	 * @return A {@link ca.umanitoba.me.car.Backend} that can be polled for data.
	//	 */
	//	static Backend getBackend()
	//	{
	//		return theInstance.backend;
	//	}

	synchronized void addPushDelegate(BajaUserInterface newReceiver)
	{
		pushDelegates.add(newReceiver);
	}

	synchronized void removePushDelegate(BajaUserInterface receiver)
	{
		pushDelegates.remove(receiver);
	}

	/**
	 * Set a client to control the <code>Backend</code>. Only one client may
	 * control the <code>Backend</code>, or else the system will become inconsistent.
	 * However, multiple clients may observe the backend. 
	 * 
	 * <p>If this method returns <code>false</code>, then the backend already has a controller and
	 * commands should not be issued.
	 * @param hostString A client address that wishes to control the <code>Backend</code>.
	 * @return <code>true</code> if <code>controller</code> gets to be the controller, or is already 
	 * the controller. <code>false</code> if <code>controller</code> is only allowed to observe. 
	 */
	synchronized boolean setController(String hostString)
	{
		//TODO: Make this actually work yo
		boolean isController = false;
		if (null != hostString)
		{
			System.out.println(hostString + " wants to be a controller");
			if (null == this.controllerID) 
			{
				this.controllerID = hostString;
				isController = true;
			} else if (hostString.equals(this.controllerID))
			{
				isController = true;				
			}

			System.out.println(this.controllerID + " is currently the controller");

		} else 
		{
			isController = false;
		}

		return isController;
	}

	/**
	 * A client wants to resign control of the <code>Backend</code>. Only one client may
	 * control the <code>Backend</code>, or else the system will become inconsistent.
	 * However, multiple clients may observe the backend. Another client may be granted
	 * control at this point.
	 * @param hostString An address of a client that wishes to give up control of the <code>Backend</code>.
	 */
	synchronized void removeController(String hostString)
	{
		System.out.println(hostString + " wants to resign being controller");
		if (hostString.equals(this.controllerID))
		{
			this.controllerID = null;
		}
	}

	/*           expose backend to Websockets        */
	/**
	 * Initialize sensors and begin taking readings. (Use {@link #startLog()} 
	 * or {@link #startLog(long)} to begin logging data to external storage)
	 */
	public synchronized void startSensors()
	{
		backend.startSensors();
	}

	/**
	 * release sensors and stop getting readings. (Use {@link #stopLog()} to
	 * just stop logging to storage).
	 */
	public void stopSensors()
	{
		backend.stopSensors();
	}

	/**
	 * Start logging data at the default sampling interval
	 */
	public void startLog()
	{
		backend.startLog();
	}

	/**
	 * Start logging data at a custom sampling interval
	 * @param sampleInterval sampling interval to use
	 */
	public void startLog(long sampleInterval)
	{
		backend.startLog(sampleInterval);
	}

	/**
	 * Stop logging data
	 */
	public void stopLog()
	{
		backend.stopLog();
	}

	/**
	 * Called by UI to request sensor data for any attached sensors. Polls the 
	 * <code>SensorManager</code> for its current sensor data.
	 * @return an array of <code>double</code>s where each index corresponds to a data value we may want to view.
	 * This data should correspond to the sensor names returned by {@link #getSensorNames()}.
	 */
	public double[] getSensorData()
	{
		return backend.getSensorData();
	}

	/**
	 * Called by the UI to request the names of all supported sensors. This allows SensorManager to
	 * be extended without needing to change the UI.
	 * @return an array of <code>String</code>s where the <code>String</code> at each index contains the name
	 * of a sensor. These names should correspond to the data in the array returned by {@link #getSensorData()}.
	 */
	public String[] getSensorNames()
	{
		return backend.getSensorNames();
	}

	/**
	 * Begin executing the set profile. If a profile is already executing, start again.
	 */
	public void startProfile()
	{
		backend.startProfile();
	}

	/**
	 * Stop executing the currently running profile. (if no profile is running, do nothing)
	 */
	public void stopProfile()
	{
		backend.stopProfile();
	}

	/**
	 * Call to set the path to the profile that we want to use. Parse if necessary to determine
	 * the servo commands needed.
	 * @param profileFilePath a <code>String</code> representing the complete path to the profile file.
	 * @return <code>true</code> if a properly formatted profile was found at <code>profileFilePath</code>, false otherwise.
	 */
	public boolean setProfileFilePath(String profileFilePath)
	{
		return backend.setProfileFilePath(profileFilePath);
	}
	
	/**
	 * Call to set the path to the profile that we want to use. Parse if necessary to determine
	 * the servo commands needed.
	 * @param profileFilePath a <code>String</code> representing the complete path to the profile file.
	 * @param speed the speed (m/s) at which to start executing the steering maneuver
	 * @return <code>true</code> if a properly formatted profile was found at <code>profileFilePath</code>, false otherwise.
	 */
	public boolean setProfileFilePath(String profileFilePath, double speed) 
	{
		return backend.setProfileFilePath(profileFilePath, speed);
	}

	/**
	 * @return An array containing paths to all known profiles. This can be displayed to the user
	 * to allow them to select one.
	 */
	public File[] getProfilePaths()
	{
		return backend.getProfilePaths();
	}

	/**
	 * Enable the automatic controller. Note that the profile will not execute until {@link #startProfile()} is called.
	 * @param automatic <code>true</code> if the car should be automatically controlled. Set to <code>false</code> if the
	 * car should be manually controlled.
	 */
	public void setAutomaticDrivingMode(boolean automatic)
	{
		backend.setAutomaticDrivingMode(automatic);
	}

	/**
	 * Rename the last file to be something different. The user might be presented with a popup that allows
	 * them to specify a filename for their log.
	 * @param newFileName
	 * @return <code>true</code> if the file was successfully renamed, <code>false</code> 
	 * if it failed or if the last file = null.
	 */
	public boolean renameLastLogFile(String newFileName)
	{
		return backend.renameLastLogFile(newFileName);
	}

	/**
	 * Verify that a volume exists to log data to. Call this function when your UI attaches to this <code>Backend</code>.
	 * This function may allow any data loggers to cache mount points, and lead to faster initialization of the log file.
	 * @return <code>true</code> if the data logging volume is attached.
	 */
	public boolean checkOutputVolume()
	{
		return backend.checkOutputVolume();
	}


}
