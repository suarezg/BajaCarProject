var dataSocket; // WebSocket
var camSocket; // WebSocket
var cmdSocket; // WebSocket

var sendingData; // boolean
var isLogging; // boolean
var didStartLogging; // boolean
var isRunningProfile // boolean

var serverIP = "130.179.229.130";
//var serverIP = "localhost"; 

var time = new Date().getTime();
var intervals = [5];

var timeWindow = 5000; //ms
var streamPort = 81;
var dataLength = 70;

var watchdogInterval = 1600; //ms

var profileFilePaths; // array

var sensorNames = [];
	
function setup()
{
	// instance variables
	sendingData = false;
	
	isLogging = false;
	didStartLogging = false;
	
	isRunningProfile = false;
	
	// format stuff
	formatControlButton();
	
	// format charts	
	formatDisplayChannels();
	
	// set UI Elements
	formatWindowField();
	formatLogDataButton();
	formatLoggingStatus();
	formatConnectionStatus(false);
	formatStartStopProfileButton();
		
	// web socket
	openDataWebSocket();	
	openCommandWebSocket("getsensornames");
	startWatchdog();
	
	setupVideo();
	//setDrivingMode(); // notify server to default to manual
}
var watchdogWorker; // type = Worker
function startWatchdog()
{
	console.log("Hello!");
	if(typeof(Worker) !== "undefined") {
         if(typeof(watchdogWorker) == "undefined") {
             watchdogWorker = new Worker("watchdog.js");
         }
         watchdogWorker.onmessage = function(event) 
		 {
            halt();
         };
		 
		 
		 watchdogWorker.postMessage({'serverIP':   serverIP, 'timeout': watchdogInterval});
     } else {
         alert("No Web Worker support. Watchdog timer is disabled!");
    }
}

function setupVideo()
{
	var videoContainer = document.getElementById("videoContainer");
	videoContainer.innerHTML = '<img id="video-frame" src="http://' + serverIP + ':' + streamPort + '/?action=stream">';
	//videoContainer.innerHTML = '<img id="video-frame" src="serenity.jpg">';
}

// handle resize events 
$(window).resize(function()
{
	resizeGraphs();
});

/** BUTTON EVENTS **/
function setDrivingMode()
{
	var toggleSwitch = document.getElementById("drivingmodeonoffswitch");
	if (toggleSwitch.checked)
	{
		sendCommand("drivingmode;computer");
	} else
	{
		sendCommand("drivingmode;manual");
	}
}

function selectFile()
{
	// from http://www.htmlgoodies.com/html5/tutorials/introducing-html-5-web-workers-bringing-multi-threading-to-javascript.html#fbid=EWQ6lsVfOHb
	var worker = new Worker('startUpload.js');
		
    worker.onmessage = function(e) 
	{
		fileSelected();
    };
    worker.onerror = function(e) {
      alert('Error: Line ' + e.lineno + ' in ' + e.filename + ': ' + e.message);
    };

    //start the worker
    worker.postMessage({'cmd':   'startUpload', 'value': document.getElementById("fileToUpload")});
}

function selectProfile()
{
	sendCommand("getprofiles");
}

function didSelectProfile(profilePath, profileName, speed)
{
	sendCommand("profile;" + profilePath + ";" + speed);
	document.getElementById("selectedprofile").innerHTML = profileName;
	setSelectProfilePopup(false);
}

function addGraph()
{
	setChannelSelectPopup(true);
}

// change data window from server (does not affect logging)
// if delta > 0, go faster. if delta < 0, go slower
function changeDataRate(delta)
{	
	timeWindow +=delta;
	if (timeWindow < 2000)
	{
		timeWindow = 2000;
	}
	
	if (timeWindow > 10000)
	{
		timeWindow = 10000;
	}

	if (sendingData)
	{
		var rate = Math.floor(timeWindow / dataLength);
		dataSocket.send("rate:" + rate);
	}
	
	formatWindowField();
	resizeGraphs();
}

function downloadData()
{

	alert("Data download coming soon!");
/*
	var sendString = ""
	if(!cmdSocket)
	{
		openCommandWebSocket(sendString);
	} else
	{
		cmdSocket.send(sendString);
	}
*/
}

// enable/disable logging
function toggleLogging()
{
	var sendString;
	if (didStartLogging)
	{
		didStartLogging = false;		
		sendString = "logstop";
	} else
	{
		didStartLogging = true;		
		sendString = "logstart";
	}
	
	sendCommand(sendString);
	
	formatLogDataButton();
}

// Enable/disable incoming data
function toggleStreaming()
{
	var sendString;
	if (sendingData)
	{
		sendingData = false;		
		sendString = "stop";
	} else
	{
		sendingData = true;	
		var rate = Math.floor(timeWindow / dataLength);		
		sendString = "rate:" + rate;
	}
	
	if(!dataSocket)
	{
		openDataWebSocket();
		
		dataSocket.onopen = function()
		{
			// Web Socket is connected, send data using send()
			dataSocket.send(sendString);
		};
	} else
	{
		dataSocket.send(sendString);
	}
	
	
	formatControlButton();
}

function toggleProfileExecution()
{
	if(isRunningProfile)
	{
		sendCommand("stopprofile");
	} else
	{
		isRunningProfile = true;
		sendCommand("startprofile");
	}
	formatStartStopProfileButton();
}


function finishedChoosingChannels()
{
	setChannelSelectPopup(false);
	formatDisplayChannels();
}

/** SERVER EVENTS **/

// new data to plot
function plotNewData(msg)
{
	var val; // array of floats
	if (msg instanceof ArrayBuffer)
	{	
		var view = new DataView(msg);		
		var val = msgpack.decode(msg);
		var time = val[0];
		
		plotNewDataInGraph(val.slice(1), time);
	}
}

// server event
function handleServerEvent(cmd)
{
	if (cmd instanceof ArrayBuffer)
	{
		parseDataArray(cmd);
		return;
	}

	var splitCmd = cmd.split(";");
	
	switch(splitCmd[0])
	{
		case "started log":
			formatLoggingStatus(true);
			break;
			
		case "no output volume":
			alert("Please attach a drive called '<code>BAJA_DISK</code>'.");
		case "finished log":
			formatLoggingStatus(false);
			break;
			
			/*
		case "hi": // expect 'ack X' where 'X' is a sequence number
			var receivedNum = parseInt(/\d+/.exec(cmd));
			
			if (seqNum === receivedNum)
			{
				didRespond = true;
				resetWatchdog();
			} // else disconnect yo
			
			break;
			*/
			
		case "paths":
			profileFilePaths = splitCmd.slice(1);
			setProfileFiles(profileFilePaths);
			setSelectProfilePopup(true);
			break;
		
		case "finished profile":
			isRunningProfile = false;
			formatStartStopProfileButton();
			break;
		
		default:
			alert(cmd);
			break;		
	}
}

function parseDataArray(data)
{
	var view = new DataView(data);		
	var val = msgpack.decode(data);
	switch(val[0])
	{
		case "sensornames":
			sensorNames = val.slice(1);
			formatChannelSelectForm();
			formatDisplayChannels();
			break;
			
		default:
			break;
	}
	
}

function handleServerEventREGEX(cmd)
{

	// trick I got from http://stackoverflow.com/questions/2896626/switch-statement-for-string-matching-in-javascript

	switch(true)
	{
		case /started log/.test(cmd):
			formatLoggingStatus(true);
			break;
			
		case /no output volume/.test(cmd):
			alert("Please attach a drive called '<code>BAJA_DISK</code>'.");
		case /finished log/.test(cmd):
			formatLoggingStatus(false);
			break;
			
		case /hi/.test(cmd): // expect 'ack X' where 'X' is a sequence number
			var receivedNum = /\d+/.exec(cmd);
			
			if (seqNum === receivedNum)
			{
				sendHeartbeat();
			} // else disconnect yo
			
			break;
			
		case /paths/.test(cmd):
			var paths = cmd.split(";");
			profileFilePaths = paths.slice(1);
			setProfileFiles(profileFilePaths);
			setSelectProfilePopup(true);
			break;
		
		default:
			alert(cmd);
			break;		
	}
}

// close websockets
function halt()
{
	//alert("halt");
	if (null !== cmdSocket)
	{
		cmdSocket.onclose = function(){}; // disable callback
		cmdSocket.close();
		cmdSocket = null;
	}
	
	if (null !== dataSocket)
	{
		dataSocket.onclose = function(){}; // disable callback
		dataSocket.close();
		dataSocket = null;
	}
	
	formatConnectionStatus(false);
	
	alert("Connection dropped!", true);
	
	formatIsAutomaticToggle(false);
	//stopWatchdog();
}

/** SETUP FUNCTIONS **/
// this websocket receives event data from the server and allows the user to send commands to the server.
function openCommandWebSocket(command)
{
	// web socket
	if ("WebSocket" in window)
	{
		// Let us open a web socket
		cmdSocket = new WebSocket("ws://" + serverIP + ":8079/cmd");
		cmdSocket.onopen = function()
		{
			formatConnectionStatus(true);
			
			if (null != command)
			{
				cmdSocket.send(command);
			}
		};
		cmdSocket.onmessage = function (evt) 
		{
			handleServerEvent(evt.data);
		};
		
		cmdSocket.onclose = function()
		{ 			
			// websocket is closed.
			//alert("Command Web Socket is closed..."); 
			formatConnectionStatus(false);
			formatIsAutomaticToggle(false);
			
			cmdSocket = null;
		};
		
		cmdSocket.binaryType = "arraybuffer"; // use ArrayBuffer instead of "blob" because we want to parse an array of bytes
	}
	else
	{
		// The browser doesn't support WebSocket
		alert("WebSocket NOT supported by your Browser!");
	}
}

function getSensorNames()
{
	
}

// this websocket receives raw data from the server
function openDataWebSocket()
{
	// web socket
	if ("WebSocket" in window)
	{
		// Let us open a web socket
		dataSocket = new WebSocket("ws://" + serverIP + ":8079/sock");
		dataSocket.onopen = function()
		{
			// Web Socket is connected, send data using send()
		};
		dataSocket.onmessage = function (evt) 
		{ 
			var received_data = evt.data;
			plotNewData(received_data);
			
			var newTime = new Date().getTime();
			var delta = newTime - time;
			time = newTime;

			//plotInterval(delta); 			
		};
		dataSocket.onclose = function()
		{ 
			// websocket is closed.
			//alert("Data connection is closed..."); 
			dataSocket = null;
		};
		
		dataSocket.binaryType = "arraybuffer"; // use ArrayBuffer instead of "blob" because we want to parse an array of bytes
	}
	else
	{
		// The browser doesn't support WebSocket
		alert("WebSocket NOT supported by your Browser!");
	}
}

function sendCommand(cmd)
{
	if(!cmdSocket)
	{
		openCommandWebSocket(cmd);
	} else
	{
		cmdSocket.send(cmd);
	}
}

/** UI STYLING **/
// don't use popups because they stink
function alert(text, type)
{
	if(!type)
	{
		toastr.info(text);
	} else
	{
		toastr.error(text);
	}
	
	//var msg = document.getElementById("msg");
	//msg.innerHTML += "<p>" + text + "</p>";
}

function formatControlButton() // this button will be removed
{
	var controlButton = document.getElementById("toggle-streaming-button");
	if (sendingData && controlButton)
	{
		controlButton.innerHTML = "Stop Data Stream";
		controlButton.className = "alert";
	} else
	{
		controlButton.innerHTML = "Start Data Stream";
		controlButton.className = "normal";
	}
}

function formatIsAutomaticToggle(isAutomatic)
{
	// var toggleSwitch = document.getElementById("drivingmodeonoffswitch");
	// toggleSwitch.checked = isAutomatic;
}

function formatLogDataButton()
{
	var controlButton = document.getElementById("toggle-logging-button");
	if (didStartLogging && controlButton)
	{
		controlButton.innerHTML = "Stop Logging";
	} else
	{
		controlButton.innerHTML = "Start Logging";
	}
}

function formatLoggingStatus(isNowLogging)
{
	var loggingStatusElement = document.getElementById("logging-status");
	
	if (isNowLogging)
	{
		didStartLogging = true;
		loggingStatusElement.innerHTML = '<span class="alert">Logging...</span>';
		formatLogDataButton();
	} else
	{
		didStartLogging = false;
		loggingStatusElement.innerHTML = '<span class="normal">Idle</span>';
		formatLogDataButton();
	}
}

function formatWindowField()
{
	var timeWindowField = document.getElementById("windowField");
	var timeWindowSecs = timeWindow / 1000; // timeWindow is in ms
		
	timeWindowField.innerHTML = (Math.round(10 * timeWindowSecs) / 10) + " s";
}

function formatConnectionStatus(isConnected)
{
	var connectionStatusBox = document.getElementById("connectionStatus");
	if (isConnected)
	{
		connectionStatusBox.innerHTML = '<span class="normal">Connected</span>';
	}
	else
	{
		connectionStatusBox.innerHTML = '<span class="alert">Not Connected</span>';
	}
}

function formatStartStopProfileButton()
{
	var startStopButton = document.getElementById("startstopprofile_button");
	if(isRunningProfile)
	{
		startStopButton.innerHTML = "Stop Profile"
		startStopButton.className = "alert";
	} else
	{
		startStopButton.innerHTML = "Start Profile"
		startStopButton.className = "normal";
	}
}



function formatChannelSelectForm()
{
	var channelSelectForm = document.getElementById("channelselectcheckboxes");
	channelSelectForm.innerHTML = "";
	for (var channelIndex = 0; channelIndex < sensorNames.length; channelIndex++)
	{
		channelSelectForm.innerHTML += '<input type="checkbox" checked class="channel" value="' + sensorNames[channelIndex] + '">' + sensorNames[channelIndex] + '</input> <br>';
	}
}

function formatDisplayChannels()
{
	var graphsDiv = document.getElementById("graphs");
	var channelSelectBoxes = document.getElementsByClassName("channel");
	graphsDiv.innerHTML = "";
	for (var checkBoxIndex = 0; checkBoxIndex < channelSelectBoxes.length; checkBoxIndex++)
	{
		if (channelSelectBoxes[checkBoxIndex].checked)
		{
			var sensorName = "graph" + checkBoxIndex; //channelSelectBoxes[checkBoxIndex].value;
			graphsDiv.innerHTML += '<div class="datagraph-container"><div class="graph-title">' + channelSelectBoxes[checkBoxIndex].value + '</div><div id="' + sensorName + '" class="datagraph" data-channel=' + checkBoxIndex + '></div></div>'
		}
	}
	// $(".graph-title").fitText();
	resizeGraphs();
}
