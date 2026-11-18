package org.globaltester.simulator.device;

import java.util.ArrayList;
import java.util.List;

import org.globaltester.base.PreferenceHelper;
import org.globaltester.simulator.preferences.PreferenceConstants;

/**
 * Consolidates access too all available {@link SimulatorDeviceConnector}
 * instances and routes commands to the prioritized instance.
 * 
 * 
 */
public class SimulatorDeviceHandler implements SimulatorDeviceConnector {
	
	private static List<SimulatorDeviceConnector> connectors = new ArrayList<>();


	private static SimulatorDeviceConnector getAvailableConnectorByPriority() {
		SimulatorDeviceConnector retVal = null;
		int retValPriority = Integer.MAX_VALUE;
		for (SimulatorDeviceConnector curConnector : connectors) {
			if ((curConnector.getPriority() < retValPriority) && curConnector.isAvailable()) {
				retVal = curConnector;
				retValPriority = retVal.getPriority();
			}
		}
		if ((retVal == null) &&
			Boolean.parseBoolean(PreferenceHelper.getPreferenceValue(PreferenceConstants.PLUGIN_ID, PreferenceConstants.FAIL_WO_DEVICECONNECTOR))) {
			throw new IllegalStateException("No SimulatorDeviceConnector available! You need to install drivers and/or connect your device."); 
		}
		return retVal;
	}
	public static void registerDeviceConnector(SimulatorDeviceConnector newConnector) {
		if (!connectors.contains(newConnector)) {
			connectors.add(newConnector);
		}
	}
	
	
	/**
	 * Starts the ISO14443 Simulator as a thread.
	 */
	public void run() {
		SimulatorDeviceConnector curConnector = getAvailableConnectorByPriority();
		if (curConnector != null) {
			curConnector.run();
		}
		
	}

	/**
	 * Stop all available 
	 */
	public void stop() {
		for (SimulatorDeviceConnector curConnector : connectors) {
			curConnector.stop();
		}
		
	}
	
	public Runnable getCleanupHook() {
		return new Runnable() {
			
			@Override
			public void run() {
				stop();
			}
		};
	}

	@Override
	public boolean isAvailable() {
		SimulatorDeviceConnector curConnector = getAvailableConnectorByPriority();
		return (curConnector != null);
	}


	@Override
	public int getPriority() {
		return Integer.MAX_VALUE;
	}

}
