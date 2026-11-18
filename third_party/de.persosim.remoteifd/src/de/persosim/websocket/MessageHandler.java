package de.persosim.websocket;

/**
 * Implementations handle IFD json messages.
 * @author boonk.martin
 *
 */
public interface MessageHandler {

	/**
	 * Handles the incoming message and constructs a response.
	 * @param incomingMessage
	 * @return the response message as {@link String} or null if no response is necessary
	 */
	public String message(String incomingMessage);

	/**
	 * @return true, iff the icc is available.
	 */
	public boolean isIccAvailable();

	/**
	 * Builds an IFD_STATUS message
	 * @return
	 */
	public String getStatusMessage();

}
