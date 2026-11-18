package de.persosim.driver.connector;

import java.io.IOException;

import de.persosim.driver.connector.service.IfdConnectorImpl;
import de.persosim.driver.connector.service.IfdConnector;

/**
 * This creates {@link IfdConnectorImpl} instances to interface with a
 * native driver.
 * 
 * @author mboonk
 *
 */
public interface DriverConnectorFactory {
	/**
	 * Provides a connector if available.
	 * 
	 * @return the connector object
	 * @param contextIdentifier
	 *            the context of the connector to be returned
	 * @throws IOException
	 *             when no connector object could be created
	 */
	public IfdConnector getConnector(String contextIdentifier) throws IOException;

	/**
	 * Frees a previously created connector allocation.
	 * 
	 * @param connector
	 *            the {@link IfdConnectorImpl} to be given back
	 * @throws IllegalArgumentException
	 *             when the connector to be given back was not created by this
	 *             factory
	 */
	public void returnConnector(IfdConnector connector);

	/**
	 * Checks for availability of native driver
	 * 
	 * @return true, if a connection can be made
	 */
	public boolean isAvailable();
}
