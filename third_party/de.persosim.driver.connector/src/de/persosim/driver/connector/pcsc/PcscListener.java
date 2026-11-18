package de.persosim.driver.connector.pcsc;

/**
 * Classes that want to be notified about PCSC data packets have to implement
 * this interface.
 * 
 * @author mboonk
 * 
 */
public interface PcscListener {
	/**
	 * Process the given {@link PcscCallData} and return the result.
	 * 
	 * @param data
	 *            the data received from the PCSC system
	 * @return the {@link PcscCallResult} data or null
	 */
	public abstract PcscCallResult processPcscCall(PcscCallData data);
}
