package ca.umanitoba.me.ui.web;

import javax.servlet.annotation.WebServlet;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;


@SuppressWarnings("serial")
@WebServlet(name="My WebSocket Servlet", urlPatterns={"/sock"})
public class SensorWebSocketServlet extends WebSocketServlet 
{

	/* (non-Javadoc)
	 * @see org.eclipse.jetty.websocket.servlet.WebSocketServlet#configure(org.eclipse.jetty.websocket.servlet.WebSocketServletFactory)
	 */
	@Override
    public void configure(WebSocketServletFactory factory) {
		
		System.out.println("configure SensorWebSocketServlet");
        factory.getPolicy().setIdleTimeout(10000); // timeout connection if nothing happens after 10 seconds
        //factory.register(MyWebSocket.class);
        factory.register(SensorWebSocket.class);
    }
}