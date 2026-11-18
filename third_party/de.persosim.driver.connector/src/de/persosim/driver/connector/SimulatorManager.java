package de.persosim.driver.connector;

import org.globaltester.simulator.Simulator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

public class SimulatorManager {
	private static Simulator simulator = null;
	private static ServiceTracker<Simulator, Simulator> serviceTrackerSimulator;
	
	public static void setSimulator(Simulator simulator) {
		SimulatorManager.simulator = simulator;
	}
	
	/**
	 * @return the OSGi-provided simulator service or null if it is not available
	 */
	public static Simulator getSim() {
		if (serviceTrackerSimulator != null && simulator == null){
			return serviceTrackerSimulator.getService();
		}
		return simulator;
	}

	public static void startOsgiAccess(BundleContext context) {
		serviceTrackerSimulator = new ServiceTracker<Simulator, Simulator>(context, Simulator.class.getName(), null);
		serviceTrackerSimulator.open();
	}
	
	public static void stopOsgiAccess(){
		serviceTrackerSimulator.close();
	}
}
