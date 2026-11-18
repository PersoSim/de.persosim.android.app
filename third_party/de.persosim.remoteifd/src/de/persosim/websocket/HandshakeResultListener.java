package de.persosim.websocket;

/**
 * This listener will be called if the tls handshake has been completed.
 * 
 * @author boonk.martin
 *
 */
public interface HandshakeResultListener {
	/**
	 * @param success
	 *            true, iff the tls handshake was completed successfully
	 */
	public void onHandshakeFinished(boolean success);
	
	/**
	 * Is called when the handshake connection is closed
	 */
	public void onConnectionClosed();
}
