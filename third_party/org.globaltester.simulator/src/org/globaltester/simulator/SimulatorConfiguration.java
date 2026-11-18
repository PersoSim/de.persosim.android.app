package org.globaltester.simulator;

public class SimulatorConfiguration {
	private static final String PROPERTY_NAME_SIMULATOR_PAYLOAD_SIZE = "com.secunet.globaltester.simulator.legacy.payload.maxSize";
	
	public static final int MAXIMUM_PAYLOAD_SIZE=4000;

	public static int getMaxPayloadSize(){
		return Integer.parseInt(System.getProperty(PROPERTY_NAME_SIMULATOR_PAYLOAD_SIZE, MAXIMUM_PAYLOAD_SIZE + ""));
	}
}
