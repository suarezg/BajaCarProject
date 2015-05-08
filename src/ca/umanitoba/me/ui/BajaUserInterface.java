package ca.umanitoba.me.ui;

import ca.umanitoba.me.car.Backend;

public interface BajaUserInterface 
{
	public enum UIEvent
	{
		/*
		 * Extend as needed...
		 */
		
		DID_FINISH_PROFILE,
		COULD_NOT_FIND_OUTPUT_VOLUME,
		COULD_NOT_FIND_PROFILE,
		INVALID_PROFILE,
		STARTED_LOG,
		FINISHED_LOG
	}
	
	/**
	 * Call this function to send a notification to the UI to display something to the user.
	 * @param event
	 */
	public void notifyUI(UIEvent event);
	
	/** Start executing the UI thread */
	public void startUI();
	
	/** 
	 * Associate a Backend to this User Interface
	 * @param be the backend
	 */
	public void setBackend(Backend be);
}
