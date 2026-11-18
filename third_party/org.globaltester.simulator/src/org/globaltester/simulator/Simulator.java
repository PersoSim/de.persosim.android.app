package org.globaltester.simulator;

/**
 * This interface defines the methods used for controlling the simulator. It is
 * used in the OSGi service definitions.
 * 
 * @author mboonk
 *
 */
public interface Simulator {

	/**
	 * This method handles instantiation and start of the SocketSimulator.
	 * Calling this method on an already running simulator does nothing.
	 * 
	 * After successfully starting the Simulator the simulator is able to
	 * process administrative FF apdus.
	 * 
	 * @return whether instantiation and starting was successful
	 */
	public abstract boolean startSimulator();

	/**
	 * Stops the simulator thread and returns when the thread is stopped.
	 * Calling this method on an already stopped simulator does nothing.
	 * @return whether stopping was successful
	 */
	public abstract boolean stopSimulator();

	/**
	 * This method restarts the simulator. It works on independent of running state.
	 * @return whether restarting has been successful
	 */
	public abstract boolean restartSimulator();

	/**
	 * @return whether the simulator is currently active
	 */
	public abstract boolean isRunning();
	
	/**
	 * Handles APDUs. Control APDUs are filtered and the respective methods of
	 * the kernel are called. All other APDUs are simply forwarded to the
	 * kernels process() method.
	 * 
	 * @param apdu
	 * @return
	 */
	public abstract byte[] processCommand(byte[] apdu);

	/**
	 * This method powers the simulated card up. It is equivalent to moving a
	 * card to the readers field.
	 * 
	 * @return the ATR of the simulated card
	 */
	public abstract byte [] cardPowerUp();
	
	/**
	 * This method powers the simulated card down. It is equivalent to removing
	 * a card from the readers field.
	 * 
	 * @return the status word
	 */
	public abstract byte [] cardPowerDown();
	
	/**
	 * This method resets the simulated card.
	 * @return the ATR of the simulated card
	 */
	public abstract byte [] cardReset();
	
	/**
	 * Register new {@link SimulatorEventListener} to this object
	 * @param newListeners
	 */
	public abstract void addEventListener(SimulatorEventListener... newListeners);
	
	/**
	 * Unregister {@link SimulatorEventListener} from this object.
	 * @param oldListener
	 */
	public abstract void removeEventListener(SimulatorEventListener oldListener);

}
