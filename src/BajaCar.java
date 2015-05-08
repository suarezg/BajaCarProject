import ca.umanitoba.me.car.Backend;
import ca.umanitoba.me.car.impl.BajaBackend;
import ca.umanitoba.me.carcontroller.impl.ServoManager;
import ca.umanitoba.me.sensors.impl.DigitalOutput;
import ca.umanitoba.me.ui.BajaUserInterface;
import ca.umanitoba.me.ui.cmd.CommandUI;
import ca.umanitoba.me.ui.web.BajaWebServerUI;


public class BajaCar {

	public static void main(String[] args)
	{
		//BajaUserInterface uInterface = new CommandUI();
		DigitalOutput.setIsReadyLED(false);
		BajaUserInterface uInterface = BajaWebServerUI.getTheInstance();
		Backend backend = new BajaBackend();
		backend.setUserInterface(uInterface);
		uInterface.setBackend(backend);

		uInterface.startUI();

		// these calls can be made from the UI but we will just do them automatically for now
		backend.startSensors();

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() 
			{
				DigitalOutput.setIsReadyLED(false);
				DigitalOutput.setAutomaticControl(false);
				ServoManager.haltSpeed();
				System.out.println("*** END ***");
			}
		});

	}
}
