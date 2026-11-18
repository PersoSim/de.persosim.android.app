package org.globaltester.simulator.device;

public interface SimulatorDeviceConnector {

	/**
	 * Starts the SimulatorDeviceConnector as a thread.
	 */
	void run();

	/**
	 * Stops the SimulatorDeviceConnector thread.
	 * 
	 * @throws CLTOneException if there was an error during handling of the CTL One
	 */
	void stop();

	/**
	 * Checks whether the used interface is available.
	 * This should check that all required preconditions are met (e.g. drivers available, connections configured, devices responding).
	 * Returns false if no
	 * actual interface is available.
	 */
	boolean isAvailable();

	/**
	 * Return priority of this implementation. Lowest value means highest priority
	 * @return
	 */
	int getPriority();

}