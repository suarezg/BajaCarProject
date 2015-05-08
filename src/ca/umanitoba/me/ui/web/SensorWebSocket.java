package ca.umanitoba.me.ui.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;
import org.msgpack.MessagePack;
import org.msgpack.packer.Packer;

import ca.umanitoba.me.car.Backend;

/**
 * @author Paul
 *
 */

public class SensorWebSocket implements WebSocketListener
{
	private Session outboundSession;
	private BajaWebServerUI backend;
	private Timer sendDataTimer;	
	private int datainterval = 50; // milliseconds

	
	public SensorWebSocket()
	{
		System.out.println("Construct SensorWebSocket...");
		sendDataTimer = new Timer();
		backend = BajaWebServerUI.getTheInstance();
		
		System.out.println("Sensors found:");
		for (String sensorNameString : backend.getSensorNames()) 
		{
			System.out.print(sensorNameString + ", ");
		}
		System.out.println();
	}
	

	
	public void sendString(String stringData)
	{
		synchronized (outboundSession) 
		{
			if (null!=outboundSession && outboundSession.isOpen()) 
			{
				outboundSession.getRemote().sendString(stringData, null);
			}	
		}
		
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
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onWebSocketClose(int arg0, String arg1) 
	{
		synchronized (outboundSession) 
		{
			System.out.println("Disconnect from: " + outboundSession.getRemoteAddress());
			this.outboundSession = null;
			System.out.println("Stopping profile...");
			backend.stopProfile();
		}				
	}

	@Override
	public void onWebSocketConnect(Session newSession)
	{
		System.out.println("Sensor Web socket connect");
		this.outboundSession = newSession;
	}

	@Override
	public void onWebSocketError(Throwable arg0)
	{
		stopUpdater();
		System.out.println("error from: " + outboundSession.getRemoteAddress());
		System.out.println("Stopping profile...");
		backend.stopProfile();
	}

	@Override
	public void onWebSocketText(String message) 
	{			
		System.out.println("sensorWebSocket on Thread " + Thread.currentThread().getName() + ": " + message);
		
		if (message.equals("start"))
		{
			startUpdater(datainterval);
		} else if (message.equals("stop"))
		{
			stopUpdater();
		} else if (("rate:".length() < message.length()) && 
				(message.substring(0,"rate:".length()).equals("rate:")))
		{
			String rateString = message.substring("rate:".length());
			try {
				datainterval= Integer.parseInt(rateString);
				startUpdater(datainterval);
				System.out.println("Updated interval to: " + datainterval + "ms");
				
				
			} catch (NumberFormatException e) {
				System.err.println("Could not parse an integer in: " + rateString + ". interval is " + datainterval + "ms");
			}					
		}
		
		
		else 
		{
			System.err.println("unrecognized data: " + message);
		}
	}
		
	/**
	 * Schedule a timer to poll the backend for data
	 * @param intervalMS time interval (ms)
	 */
	private void startUpdater(int intervalMS)
	{
		TimerTask sendDataTask = new TimerTask() 
		{
			boolean firstTime = true; // first time executing the thread
			long startDateMillis = new Date().getTime();
			
			@Override
			public void run()
			{
//				if (firstTime) 
//				{
//					System.out.print("new byte array stream...");
//				}
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
//				if (firstTime) 
//				{
//					System.out.println("done");
//				}
//				if (firstTime) 
//				{
//					System.out.print("new MessagePack()...");
//				}
				MessagePack messagePack = new MessagePack();
//				if (firstTime) 
//				{
//					System.out.println("done");
//				}
//				if (firstTime) 
//				{
//					System.out.print("createPacker()...");
//				}
				Packer packer = messagePack.createPacker(stream);
//				if (firstTime) 
//				{
//					System.out.println("done");
//				}

				double[] sensorData = backend.getSensorData(); // grab data
				
				try {
					packer.writeArrayBegin(sensorData.length + 1);
					double time = ((new Date().getTime()) - startDateMillis) / 1000.0;
					packer.write(time);
					
					for (double datum : sensorData)
					{
						packer.write(datum);
					}
					
					packer.writeArrayEnd(false);
					
					packer.close();
				} catch (IOException e) {
					e.printStackTrace();
				}		
				
				
				sendBytes(stream.toByteArray());
				if (firstTime)
				{
					//System.out.println("done first time.");
					firstTime = false;
				}
				
				//System.out.println("Accel: " + accelData[0] + "\ngyro: "+ gyroData[0] + "\ncompass: " + compassData[0]);
			}
		};
		
		if(null != sendDataTimer)
		{
			sendDataTimer.cancel();
			
		}
		System.out.print("Start send data timer...");
		sendDataTimer = new Timer("Polling task (UI Web Socket)");
		sendDataTimer.scheduleAtFixedRate(sendDataTask, 0, intervalMS);
		System.out.println("done.");
	}
	
	/**
	 * stop polling for data
	 */
	private void stopUpdater()
	{
		sendDataTimer.cancel();
	}
}
