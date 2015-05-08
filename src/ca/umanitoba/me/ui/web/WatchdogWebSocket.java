/**
 * 
 */
package ca.umanitoba.me.ui.web;

import java.util.Timer;
import java.util.TimerTask;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketListener;

import ca.umanitoba.me.car.Backend;

/**
 * @author Paul
 * @version 0.1
 *
 */
public class WatchdogWebSocket implements WebSocketListener 
{
	private volatile Timer watchdog;
	private Session outboundSession;
	private BajaWebServerUI backend;
	
	private static final long WATCHDOG_TIMEOUT_MS = 2000;

	private void terminate()
	{		
		if (null != watchdog)
		{
			watchdog.cancel();
		}

		watchdog = null;
		backend.stopProfile();
		backend.stopLog();
		backend.setAutomaticDrivingMode(false);
	}
	
	private void resetWatchdog()
	{
		if (null != watchdog)
		{
			watchdog.cancel();
		}

		watchdog = null;
		watchdog = new Timer("Watchdog Timer: " + outboundSession.getRemoteAddress().getHostString());



		watchdog.schedule(new TimerTask() {

			@Override
			public void run() 
			{
				System.out.println("MISSED WATCHDOG! TERMINATE!");
				terminate();
			}
		}, WATCHDOG_TIMEOUT_MS);

	}

	public void sendString(String stringData)
	{
		if (null!=outboundSession && outboundSession.isOpen()) 
		{
//			System.out.println("Sending: " + stringData);
			outboundSession.getRemote().sendString(stringData, null);
		}	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.api.WebSocketListener#onWebSocketBinary(byte[], int, int)
	 */
	@Override
	public void onWebSocketBinary(byte[] arg0, int arg1, int arg2) 
	{
		// TODO Auto-generated method stub

	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.api.WebSocketListener#onWebSocketClose(int, java.lang.String)
	 */
	@Override
	public void onWebSocketClose(int arg0, String arg1) 
	{
		System.out.println("close watchdog thread. terminate");
		terminate();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.api.WebSocketListener#onWebSocketConnect(org.eclipse.jetty.websocket.api.Session)
	 */
	@Override
	public void onWebSocketConnect(Session session) 
	{
		outboundSession = session;
		backend = BajaWebServerUI.getTheInstance();
		System.out.println("initialize watchdog websocket with timeout " + WATCHDOG_TIMEOUT_MS + "ms. (Timer not started)");
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.api.WebSocketListener#onWebSocketError(java.lang.Throwable)
	 */
	@Override
	public void onWebSocketError(Throwable arg0) 
	{
		System.err.println("watchdog websocket error. terminate");
		terminate();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.api.WebSocketListener#onWebSocketText(java.lang.String)
	 */
	@Override
	public void onWebSocketText(String message) 
	{
		System.out.println("WatchdogSocket Thread " + Thread.currentThread().getName() + " rec: " + message);
		String[] command = message.split(";");

		if ("hi".equals(command[0]))
		{
			if (command.length > 1)
			{
				resetWatchdog();
				sendString("hi;" + command[1]);
			}
		}
	}
}
