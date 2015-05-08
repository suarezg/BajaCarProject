/**
 * 
 */
package ca.umanitoba.me.carcontroller.impl;

import ca.umanitoba.me.carcontroller.SteeringController;


/**
 * Issue steering angles to the {@link ca.umanitoba.me.carcontroller.impl.ServoManager Servomanager}.
 * This is an open-loop controller and therefore does not use feedback to improve the steering response.
 * @author Paul
 *
 */
public class OpenLoopSteeringController implements SteeringController 
{
	
	private double calibrationOffset = 0;
	
	protected OpenLoopSteeringController()
	{
	}

	/* (non-Javadoc)
	 * @see ca.umanitoba.me.carcontroller.SteeringController#setSteeringAngle(float)
	 */
	@Override
	public void setSteeringAngle(float angle)
	{		
		ServoManager.setSteeringAngle(angle + calibrationOffset);
	}

	@Override
	public void setCalibrationOffset(double offset)
	{
		this.calibrationOffset = offset;
	}

}
