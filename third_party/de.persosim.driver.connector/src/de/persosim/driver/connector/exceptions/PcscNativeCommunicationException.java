package de.persosim.driver.connector.exceptions;

/**
 * This exception or subclasses should be thrown if the communication with the
 * native driver component fails.
 * 
 * @author mboonk
 *
 */
public class PcscNativeCommunicationException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PcscNativeCommunicationException(String string) {
		super(string);
	}
}
