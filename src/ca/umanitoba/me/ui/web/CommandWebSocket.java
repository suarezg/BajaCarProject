package ca.umanitoba.me.ui.web;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.AlgorithmConstraints;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import ca.umanitoba.me.car.Backend;
import ca.umanitoba.me.ui.BajaUserInterface;

public class CommandWebSocket implements WebSocketListener, BajaController, BajaUserInterface
{	
	private volatile Object sessionSemaphore;
	private Session outboundSession;

	private BajaWebServerUI backend = BajaWebServerUI.getTheInstance();

	private volatile boolean isOpen;

	private Timer pingTimer;


	public CommandWebSocket()
	{
		System.out.println("Construct CommandWebSocket...");
		sessionSemaphore = new Object();
	}

	public void sendString(String stringData)
	{

		//synchronized (sessionSemaphore) 
		//{
		try {
			if (null!=outboundSession && isOpen && outboundSession.isOpen()) 
			{
				System.out.println("Sending: " + stringData);
				outboundSession.getRemote().sendString(stringData, null);
			}	
		} catch (Exception e) {
			System.err.println("Error sending command string: " + stringData);
			e.printStackTrace();
		}
			
		//}

	}
	
	public void sendBytes(byte[] bytes)
	{
		ByteBuffer buffer = ByteBuffer.wrap(bytes);		
		synchronized (outboundSession)
		{
			if (null!=outboundSession && outboundSession.isOpen())
			{
				try 
				{
					outboundSession.getRemote().sendBytes(buffer);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	}

	@Override
	public void onWebSocketBinary(byte[] arg0, int arg1, int arg2)
	{

	}

	@Override
	public void onWebSocketClose(int arg0, String arg1)
	{	
		synchronized (sessionSemaphore) 
		{
			if (null != outboundSession) 
			{
				System.out.println("close command socket from: " + outboundSession.getRemoteAddress());
				isOpen = false;
				System.out.println("Stopping profile...");
				terminate();
			} else {
				System.err.println("OnWebSocketClose: Outbound Session is already null!");
			}

		}		
	}

	@Override
	public void onWebSocketConnect(Session newSession) 
	{
		synchronized (sessionSemaphore) 
		{
			isOpen = true;
			System.out.println("command Web socket connect");
			this.outboundSession = newSession;

			// ping timer sends pings every 500ms to keep the channel from closing
			if (null != pingTimer)
			{
				pingTimer.cancel();
			}
			pingTimer = null;
			pingTimer = new Timer("Ping timer:" + outboundSession.getRemoteAddress().getHostString());

			pingTimer.schedule(new TimerTask() {
				byte seq = 0;
				boolean keepGoing = true;
				@Override
				public void run()
				{
					synchronized (sessionSemaphore)
					{
						if (keepGoing)
						{
							ByteBuffer buffer = ByteBuffer.wrap(new byte[] {seq});
							try 
							{
								if (outboundSession.isOpen()) 
								{
									try 
									{
										outboundSession.getRemote().sendPing(buffer);
										seq++;
									} catch (WebSocketException e) 
									{
										System.out.println("Outbound session is already closed.");
										keepGoing = false;
									}
	
								} else 
								{
									System.err.println("Outbound session " + outboundSession + " was closed. no more pinging");
									if (null != pingTimer)
									{
										keepGoing = false;
									}
								}
							} catch (IOException e) // sendPing
							{
								e.printStackTrace();
								keepGoing = false;
							} catch (NullPointerException e)
							{
								System.err.println(Thread.currentThread().getName() + ": outbound session is already closed.");
								keepGoing = false;
							}		
						} else {
							this.cancel();
						}
					} // synchronized()
				} // run()

			}, 0, 50);

			this.outboundSession.setIdleTimeout(1000);
		}

		if (!backend.setController(this.outboundSession.getRemoteAddress().getHostString()))
		{
			sendString("observer");
		}
		backend.addPushDelegate(this);

		// check if output volume is attached

		boolean isOutputPresent = backend.checkOutputVolume();


		if (isOutputPresent)
		{
			sendString("output volume detected");
		} else {
			sendString("no output volume");
		}
	}

	@Override
	public void onWebSocketError(Throwable arg0) 
	{
		synchronized (sessionSemaphore) 
		{
			System.out.println("command socket error from: " + outboundSession.getRemoteAddress());
			isOpen = false;
			terminate();
		}		
	}

	@Override
	public void onWebSocketText(String message) 
	{
		System.out.println("ControlSocket Thread " + Thread.currentThread().getName() + " rec: " + message);
		String[] command = message.split(";");

		// commands for backend
		switch (command[0])
		{
		case "logstart":
			backend.startLog();
			break;

		case "logstop":
			backend.stopLog();
			break;

		case "getprofiles":
			Thread getProfilesThread = new Thread(new Runnable() {

				@Override
				public void run() {
					// TODO Auto-generated method stub
					File[] profiles = backend.getProfilePaths();
					String pathsString = "paths";
					for (File profile : profiles) 
					{
						pathsString += ";" + profile.getAbsolutePath();
					}
//					for (int pathIndex = 0; pathIndex < profilePaths.length; pathIndex++) 
//					{
//						if (pathIndex != profilePaths.length -1)
//						{
//							pathsString += profilePaths[pathIndex] + ";";
//						} else 
//						{
//							pathsString += profilePaths[pathIndex];
//						}
//					}

					sendString(pathsString);
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			getProfilesThread.start();

			break;

		case "profile":
			if (command.length > 2) 
			{
				try {
					double speed = Double.parseDouble(command[2]);
					
					boolean didSetPath = backend.setProfileFilePath(command[1], speed);
					if (didSetPath) 
					{
						System.out.println("set profile at path: " + command[1] + " at speed " + speed + " m/s");
					} else {
						System.err.println("couldn't set profile at path: " + command[1] + "; speed " + speed + " m/s");
					}
					break;
				} catch (NumberFormatException e)
				{
					System.err.println("could not parse " + command[2] + " as a double.");
					e.printStackTrace();
				}
				
			} // if (command length > 2) else
			if (command.length > 1) 
			{
				boolean didSetPath = backend.setProfileFilePath(command[1]);
				if (didSetPath) 
				{
					System.out.println("set profile at path: " + command[1]);
				} else {
					System.err.println("couldn't set profile at path: " + command[1]);
				}
				break;
			} // if (command length > 1) else
			break;

		case "startprofile":
			backend.startProfile();
			break;

		case "stopprofile":
			backend.stopProfile();
			break;
			
		case "getsensornames":
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			MessagePack messagePack = new MessagePack();
			Packer packer = messagePack.createPacker(stream);
			String[] sensorNames = backend.getSensorNames(); // grab names

			try {
				packer.writeArrayBegin(sensorNames.length + 1);
				packer.write("sensornames");
				for (String sensorName : sensorNames)
				{
					packer.write(sensorName);
				}

				packer.writeArrayEnd(false);
				packer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}		

			sendBytes(stream.toByteArray());
			break;


			
		case "drivingmode":
			if (command.length > 1) {
				switch (command[1])
				{
					case "computer":
						backend.setAutomaticDrivingMode(true);
						break;
						
					case "manual":
						backend.setAutomaticDrivingMode(false);
						break;
						
					default:
						break;
				}
				break;
			} // if (command length > 1)
			break;

		default:
			System.out.println("Unrecognized command: " + message);
			break;
		}
	}

	@Override
	public String getName() 
	{
		synchronized (sessionSemaphore) 
		{
			if(null != outboundSession)
			{
				return outboundSession.getRemoteAddress().getHostString();
			} else {
				return "name_not_ready";
			}
		}		
	}

	@Override
	public void notifyUI(UIEvent event)
	{
		switch (event) {
		case COULD_NOT_FIND_OUTPUT_VOLUME:
			sendString("no output volume");
			break;

		case DID_FINISH_PROFILE:
			sendString("finished profile");
			break;

		case INVALID_PROFILE:
			sendString("invalid profile");
			break;

		case STARTED_LOG:
			sendString("started log");
			break;

		case FINISHED_LOG:
			sendString("finished log");
			break;

		default:
			break;
		}
	}

	@Override
	public void startUI() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setBackend(Backend be) {
		// TODO Auto-generated method stub

	}

	private synchronized void terminate()
	{
		if (null != outboundSession)
		{
			System.out.println("terminate control socket for " + outboundSession.getRemoteAddress().getHostString());
			backend.stopProfile();
			backend.removePushDelegate(this);
			backend.removeController(this.outboundSession.getRemoteAddress().getHostString());
		}
		backend.stopProfile();
		backend.setAutomaticDrivingMode(false);
		backend.removePushDelegate(this);
		backend.removeController(this.outboundSession.getRemoteAddress().getHostString());
		outboundSession.close();
		pingTimer.cancel();

		pingTimer = null;
		backend = null;
		outboundSession = null;
	}

}
