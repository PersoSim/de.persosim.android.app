package de.persosim.driver.connector;

import java.util.List;

import de.persosim.driver.connector.pcsc.PcscListener;

public interface IfdComm {
	public void start();
	public void stop();
	boolean isRunning();
	
	/**
	 * @param listeners
	 *            the {@link PcscListener}s that need to be informed when a new
	 *            PCSC message is transmitted
	 */
	public void setListeners(List<PcscListener> listeners);
	
	public void reset();
	
	public String getName();
	
	public String getUserString();
}
