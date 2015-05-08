var watchdogSocket;
var watchdogInterval; //ms
var serverIP;


// say hello to the server, and make sure that it's still there
var seqNum = 0;
var didRespond = true;
function sendHeartbeat()
{
	seqNum = (seqNum + 1) % 10;
	
	if (didRespond)
	{		
		if(!watchdogSocket)
		{
			openWatchdogWebSocket();
		} else
		{
			watchdogSocket.send('hi;' + seqNum);
		}
		didRespond = false;
	} else
	{
		postMessage("");
		clearInterval(watchdog);
	}
}

// timeout if we don't hear back from the server
var watchdog;
function resetWatchdog()
{
	if (null !== watchdog)
	{
		clearInterval(watchdog);
	}
	
	watchdog = setInterval(function(){sendHeartbeat()}, watchdogInterval);
}

function stopWatchdog()
{
	clearInterval(watchdog);
}

// this websocket receives event data from the server and allows the user to send commands to the server.
function openWatchdogWebSocket()
{
	// web socket
	// if ("WebSocket" in window)
	// {
		// Let us open a web socket
		watchdogSocket = new WebSocket("ws://" + serverIP + ":8079/watchdog");
		watchdogSocket.onopen = function()
		{
			resetWatchdog();
		};
		watchdogSocket.onmessage = function (evt) 
		{
			var cmd = evt.data;
			var splitCmd = cmd.split(";");
			
			switch(splitCmd[0])
			{
				case "hi": // expect 'ack X' where 'X' is a sequence number

					var receivedNum = parseInt(/\d+/.exec(cmd));
					
					if (seqNum === receivedNum)
					{
						didRespond = true;
					} // else disconnect yo
					break;
					
				default:
					break;
			}
		};
		
		watchdogSocket.onclose = function()
		{ 			
			// websocket is closed.
			//alert("Watchdog Web Socket is closed..."); 
			watchdogSocket = null;
		};
		
		watchdogSocket.binaryType = "arraybuffer"; // use ArrayBuffer instead of "blob" because we want to parse an array of bytes
	// }
	// else
	// {
		// // The browser doesn't support WebSocket
	// }
}

function init(destIP, timeout)
{	
	watchdogInterval = timeout;
	serverIP = destIP;
	
	openWatchdogWebSocket();
}

self.onmessage = function(e) 
{
	init(e.data.serverIP, parseInt(e.data.timeout));
}