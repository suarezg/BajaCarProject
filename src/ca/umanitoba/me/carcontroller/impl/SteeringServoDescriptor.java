package ca.umanitoba.me.carcontroller.impl;

public enum SteeringServoDescriptor 
{
	//HPI_SFL11MG(137.0, 55.0, 350, 177, 491, 2425),
	HPI_SFL11MG(17.50, -17.5, 800, 35, 1030, 1970);
	
	
	
	// steering properties
	public final double hardRightAngle;
	public final double hardLeftAngle;
	
	// motor properties (From Phidgets calibration utility)
	public final double velocityMax;	
	public final double fullArc;
	public final double lowUS;
	public final double highUS;

	
	SteeringServoDescriptor(double hardRightAngle, double hardLeftAngle, double velocityMax,
			double fullArc, double lowUS, double highUS) 
	{
		this.hardRightAngle = hardRightAngle;
		this.hardLeftAngle = hardLeftAngle;
		
		this.velocityMax = velocityMax;
		this.fullArc = fullArc;
		this.lowUS = lowUS;
		this.highUS = highUS;
	}
}
