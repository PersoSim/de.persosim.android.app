package de.persosim.driver.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.service.IfdConnectorImpl;
import de.persosim.driver.connector.service.IfdConnector;

public class DriverConnectorFactoryImpl implements DriverConnectorFactory {

	private IfdConnector connector;
	private String contextIdentifier;

	@Override
	public IfdConnector getConnector(String contextIdentifier) throws IOException {
		if (connector == null) {
			connector = new IfdConnectorImpl();
			this.contextIdentifier = contextIdentifier;
		}
		if (this.contextIdentifier.equals(contextIdentifier)){
			return connector;	
		}
		throw new IOException("No connector can be created for the given context " + contextIdentifier);
	}

	@Override
	public void returnConnector(IfdConnector connector) {
		if (this.connector == connector) {
			try {
				connector.disconnect();
			} catch (IOException | InterruptedException e) {
				// as this is only for cleanup in case the client did not call
				// disconnect exceptions are expected
			}
			this.connector = null;
			return;
		}
		throw new IllegalArgumentException("This connector was not given out by this factory");
	}

	@Override
	public boolean isAvailable() {
		try {
			Socket commandSocket = new Socket();
			commandSocket.connect(new InetSocketAddress(VirtualDriverComm.DEFAULT_HOST, VirtualDriverComm.DEFAULT_PORT),200);
			commandSocket.close();
		} catch (IOException e) {
			BasicLogger.logException(getClass(), "PersoSim native driver unreachable", e, LogLevel.TRACE);
			return false;
		}
		
		return true;
	}
}
