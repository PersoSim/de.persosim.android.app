package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.service.IfdConnector;

public interface ConnectorEnabled {
	public abstract void setConnector(IfdConnector connector);
}
