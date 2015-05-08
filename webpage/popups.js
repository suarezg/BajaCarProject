function setUploadPopup(isShown)
{
	if(isShown)
	{
		document.getElementById("uploadpopup").style.display = "block";
		document.getElementById("dark_bkgnd").style.display = "block";
	} else
	{
		document.getElementById("uploadpopup").style.display = "none";
		document.getElementById("dark_bkgnd").style.display = "none";
	}
	
}

function setSelectProfilePopup(isShown)
{
	if(isShown)
	{
		document.getElementById("selectprofilepopup").style.display = "block";
		document.getElementById("dark_bkgnd").style.display = "block";
		
		speedInput = document.getElementById("speedValue");
		profileSelect = document.getElementById("selectprofile");
		//setOkButtonEnabled(false);
	} else
	{
		document.getElementById("selectprofilepopup").style.display = "none";
		document.getElementById("dark_bkgnd").style.display = "none";
	}
}

function setOkButtonEnabled(isEnabled)
{
	document.getElementById("didselectprofileOK").disabled = !isEnabled;
}

function profileOK()
{			
	var profileIndex = profileSelect.selectedIndex;
	var profileName = profileSelect.options[profileIndex].text;

	didSelectProfile(profileSelect.value, profileName, speedInput.value);
}

var profileSelect;
function profileSelectChange()
{
	setOkButtonEnabled(allDataReady());
}

var speedInput;
function targetSpeedDidChange()
{
	setOkButtonEnabled(allDataReady());
}

function allDataReady()
{
	return (speedInput.value && (profileSelect.selectedIndex > -1))
}

function setChannelSelectPopup(isShown)
{
	if(isShown)
	{
		document.getElementById("selectchannelpopup").style.display = "block";
		document.getElementById("dark_bkgnd").style.display = "block";
	} else
	{
		document.getElementById("selectchannelpopup").style.display = "none";
		document.getElementById("dark_bkgnd").style.display = "none";
	}
}

function setProfileFiles(files)
{
	var profileSelectList = document.getElementById("selectprofile");
	var noProfilesMessage = document.getElementById("noprofilesmessage");
	
	
	var fileListAsHTML;
	var fileName;
	var filePathComponents; // array
	
	if(files.length == 0)
	{
		noprofilesmessage.style.display = "inline";
		profileSelectList.style.display = "none";
	} else
	{
		noprofilesmessage.style.display = "none";
		profileSelectList.style.display = "inline";
		
		for (var index = 0; index < files.length; index++)
		{
			// Windows server
			filePathComponents = files[index].split("\\");
			if(filePathComponents.length == 1) // Unix file; path separator is '/'
			{
				filePathComponents = files[index].split("/")
			}	
			
			fileName = filePathComponents[filePathComponents.length - 1];
			fileListAsHTML += '<option value="' + files[index] + '">' + fileName + '</option>';
		}
	}
	
	
	
	profileSelectList.innerHTML = fileListAsHTML;
}
