package ca.umanitoba.me.ui.web;

public interface BajaController 
{
	/** 
	 * Identify the controller so that other users know who is controlling
	 * the vehicle, in case multiple clients want to control the car.
	 * @return a human-readable string to identify this client. (e.g. IP address)
	 */
	public String getName();
}
