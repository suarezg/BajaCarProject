var timeVector = [];
var data = false;

function resizeGraphs()
{
	formatCharts();
}


var plotData;

var charts; // array of charts to draw into. initialized in formatCharts()

function buildDataArray()
{
	data = [];
	for (var sensorIndex = 0; sensorIndex < sensorNames.length; sensorIndex++)
	{
		data.push([0]);
	}
}

function plotNewDataInGraph(dataNew, timeNew)
{	
	if(!data)
	{
		buildDataArray();
	}

	if (sensorNames.length === dataNew.length)
	{
		
		// add data to buffer
		for (var sensorIndex = 0; sensorIndex < dataNew.length; sensorIndex++)
		{
			data[sensorIndex].push(dataNew[sensorIndex]);
			
			while(data[sensorIndex].length > dataLength)
			{
				data[sensorIndex] = data[sensorIndex].slice(1);
			}
		}
		

		timeVector.push(timeNew);
		while(timeVector.length > dataLength)
		{
			timeVector = timeVector.slice(1);
		}
		
		// zip up
		var initialTime = timeVector[0];
		var currentTime = 0;
		
		// figure out which data actually needs to be displayed
		plotData = [];
		
		for (var channelIndex = 0; channelIndex < channelsToPlot.length; channelIndex++)
		{

			var currentChannelData = [];
			var plotChannel = channelsToPlot[channelIndex];
			for (var dataIndex = 0; dataIndex < dataLength; dataIndex ++ )
			{
				currentTime = (timeVector[dataIndex] - initialTime);
				currentChannelData.push([currentTime, data[plotChannel][dataIndex]]);
			}
			
			plotData.push(currentChannelData);
		}
		
		//var plots = document.getElementsByClassName("datagraph");
		for(var plotIndex = 0; plotIndex < charts.length; plotIndex++)
		{
			
			//var sensorIndex = plots[plotIndex].getAttribute("data-channel"); // which sensor does this plot want to show			charts[plotIndex].setData(plotData[sensorIndex]);
			charts[plotIndex].setData([plotData[plotIndex]]);
			charts[plotIndex].draw();
		}
	}	
}

function plotInterval(delta)
{
	intervals.push(delta);
	while (intervals.length > dataLength)
	{
		intervals = intervals.slice(1);
	}
	// zip up intervals
	var plotData = [];
	for (var intervalIndex = 0; intervalIndex < intervals.length; intervalIndex ++)
	{
		plotData.push([intervalIndex/dataLength, intervals[intervalIndex]]);
	}			
	
	intervalPlot.setData([plotData]);

	intervalPlot.draw();
}

var channelsToPlot = [];
function formatCharts()
{

	timeVector = [];
	channelsToPlot = [];
	//data = [[]];
	
	
	var d1 = [];
	for (var i = 0; i < 1; i += 0.05) {
		d1.push([i, Math.sin(i)]);
	}
	
	var plots = document.getElementsByClassName("datagraph");
	charts = [];
	$(".datagraph-container").css("height", ((100/(plots.length)) + "%"));
	for(var plotIndex = 0; plotIndex < plots.length; plotIndex++)
	{
		var sensorIndex = plots[plotIndex].getAttribute("data-channel"); // which sensor does this plot want to show
		channelsToPlot.push(sensorIndex);
		var plotId = "#" + plots[plotIndex].id;
		charts.push($.plot(plotId, [ d1 ], {
			colors:["#FF7070"],
			series: {
				shadowSize: 0	// Drawing is faster without shadows
			},
			yaxis: {
				min: -10,
				max: 10
			},
			xaxis: {
				show:  true,
				min: 0,
				max: timeWindow / 1000
			},
			title: plots[plotIndex].id
		}));
		
		//charts[plotIndex].setData(d1);
		//charts[plotIndex].draw();
	}
	
	var titles = document.getElementsByClassName("graph-title");
	// for (var titleIndex = 0; titleIndex < titles.length; titleIndex++)
	// {
		// titles[titleIndex].style["font-size"] = Math.max(Math.min($(this).width() / (10))) + "px";
	// }
}

function formatIntervalChart()
{
	var d1 = [];
	for (var i = 0; i < 1; i += 0.05) {
		d1.push([i, Math.sin(i)]);
	}
	
	intervalPlot = $.plot("#intervalPlot", [ d1 ], {
		series: {
			shadowSize: 0	// Drawing is faster without shadows
		},
		yaxis: {
			min: 30,
			max: 200
		},
		xaxis: {
			show: false
		}
	});
}