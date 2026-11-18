package de.persosim.driver.connector;

import java.io.IOException;

import org.globaltester.base.PreferenceHelper;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.globaltester.simulator.device.SimulatorDeviceConnector;

import de.persosim.driver.connector.exceptions.IfdCreationException;
import de.persosim.driver.connector.features.DefaultListener;
import de.persosim.driver.connector.service.IfdConnector;

/**
 * {@link SimulatorDeviceConnector} implementation to connect a Simulator with
 * PersoSim driver, if available.
 * 
 */

public class SimulatorPersoSimDriverConnector implements SimulatorDeviceConnector {
	public static final int DEFAULT_PORT = 5678;
	public static final String DEFAULT_HOST = "localhost";
	public static final String GLOBALTESTER_CONNECTOR_CONTEXT_ID = "com.secunet.globaltester";
	
	
	private static SimulatorPersoSimDriverConnector instance = null;
	
	public static synchronized SimulatorPersoSimDriverConnector getInstance() {
		if (instance == null) {
			instance = new SimulatorPersoSimDriverConnector();
		}
		return instance;
	}
	
	private SimulatorPersoSimDriverConnector() {
	}

	IfdConnector ndc;


	/**
	 * This method terminates all remainders of previous connections.
	 */
	private void cleanup() {
		
		if (ndc != null) {
			BasicLogger.log("Closing previous IfdConnector", LogLevel.INFO);
			try {
				ndc.disconnect();
				de.persosim.driver.connector.Activator.getFactory().returnConnector(ndc);
			} catch (IOException | InterruptedException e) {
				BasicLogger.logException("Error closing previous IfdConnector", e, LogLevel.ERROR);
			}
			ndc = null;
		}
	}

	@Override
	public void run() {
		cleanup();
		
		BasicLogger.log("Initialize VirtualReader to handle command APDUs", LogLevel.INFO);
		try {
			DriverConnectorFactory factory = de.persosim.driver.connector.Activator.getFactory();
			if (factory != null){
				ndc = factory.getConnector(GLOBALTESTER_CONNECTOR_CONTEXT_ID);
				ndc.addListener(new DefaultListener());
				ndc.connect(new VirtualDriverComm(DEFAULT_HOST, DEFAULT_PORT));
				BasicLogger.log("VirtualReader running, awaits command APDUs", LogLevel.INFO);
			} else {
				throw new IllegalStateException("VirtualReader could not be initalized");
			}
			
		} catch (IOException | IfdCreationException | IllegalStateException e) {

			BasicLogger.log("VirtualReader could not be initalized", LogLevel.INFO);
			BasicLogger.logException("No driver connector could be obtained, simulation only reachable via OSGI-service interface",e, LogLevel.WARN);
		}

	}

	@Override
	public void stop() {
		cleanup();
	}

	@Override
	public boolean isAvailable() {
		boolean retVal = false;
		retVal |= (ndc != null) &&  ndc.isRunning();
		
		DriverConnectorFactory factory = de.persosim.driver.connector.Activator.getFactory();
		if (factory != null) {
			retVal |= factory.isAvailable();
		}
		
		
		return retVal;
	}

	@Override
	public int getPriority() {
		// defaults to 300, which is lowest currently known priority (to prioritize hardware simulations devices when available)
		return Integer.parseInt(PreferenceHelper.getPreferenceValue(Activator.PLUGIN, "SimulatorPersoSimDriverConnectorPriority", "300"));
	}

}
