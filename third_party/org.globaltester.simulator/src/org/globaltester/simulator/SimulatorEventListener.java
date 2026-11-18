package org.globaltester.simulator;

import org.globaltester.simulator.event.SimulatorEvent;

/**
 * Listener that can be registered with {@link Simulator} objects in order to
 * become informed about specific status changes in those objects.
 * 
 * @author amay
 *
 */
public interface SimulatorEventListener {

	void notifySimulatorEvent(SimulatorEvent simEvent);

}
