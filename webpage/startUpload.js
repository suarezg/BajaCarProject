function startUpload(fileInput)
{
	//var fileInput = document.getElementById("fileToUpload");
	fileInput.click();
}

//wait for the start 'CalculatePi' message
//e is the event and e.data contains the JSON object
self.onmessage = function(e) {
  startUpload(e.data.value);
}