package ca.umanitoba.me.sensors.impl;

public enum BajaSensor
{
	/*
	 * Sensors
	 */
	
	AccelerometerX("Accelerometer X"),
	AccelerometerY("Accelerometer Y"),
	AccelerometerZ("Accelerometer Z"),

	GyroX("Gyroscope X"),
	GyroY("Gyroscope Y"),
	GyroZ("Gyroscope Z"),

	CompassX("Compass X"),
	CompassY("Compass Y"),
	CompassZ("Compass Z"),

	WheelFRSpeed0("Wheel Speed Front Right", "m/s"),
	WheelFLSpeed1("Wheel Speed Front Left", "m/s"),
	WheelBRSpeed2("Wheel Speed Back Right", "m/s"),
	WheelBLSpeed3("Wheel Speed Back Left", "m/s"),

	LongitudinalSpeed("Longitudinal Speed", "m/s");
	
	/*
	 * Groups
	 */
	
	/**
	 * Sensors from IMU Sensor
	 */
	public static BajaSensor[] imuSensors()
	{
		return new BajaSensor[] {
				AccelerometerX,
				AccelerometerY,
				AccelerometerZ,
				
				GyroX,
				GyroY,
				GyroZ,
				
				CompassX,
				CompassY,
				CompassZ
		};
	}
	
	/**
	 * Wheel speed sensors (Encoders)
	 */
	public static BajaSensor[] wheelSpeedSensors()
	{
		return new BajaSensor[] {
				WheelFRSpeed0,
				WheelFLSpeed1,
				WheelBRSpeed2,
				WheelBLSpeed3
		};
	}
	
	/*
	 * This code does not need to change after adding more sensors
	 */
	public final String descriptionString;
	public final String unitsString;
	private int index;
	
	private BajaSensor(String description)
	{
		this(description, "??");
	}
	
	private BajaSensor(String description, String units)
	{
		this.descriptionString = description;
		this.unitsString = units;
		this.index = -1;
	}
	
	/**
	 * @return The index of this sensor in the array returned by {@link ca.umanitoba.me.sensors.SensorManager#getSensorData() getSensorData()}.
	 */
	public int getIndex()
	{
		if (-1 == index)
		{
			BajaSensor[] allBajaSensors = BajaSensor.values();
			for (int i = 0; i < allBajaSensors.length; i++)
			{
				if (this == allBajaSensors[i]) 
				{
					index = i;
					break;
				}
			}
		}
		
		return index;
	}
}