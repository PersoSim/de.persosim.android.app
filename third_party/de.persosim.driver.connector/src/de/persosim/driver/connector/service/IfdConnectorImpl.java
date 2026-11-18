package de.persosim.driver.connector.service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.persosim.driver.connector.IfdComm;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.ConnectorEnabled;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.UiEnabled;

/**
 * This class handles the connection to the external part of the PersoSim interface
 * package via an {@link IfdComm}. In PCSC terms this would represent
 * functionality like an IFD Handler presents to the upper layers as described
 * in the PCSC specification part 3.
 * 
 * @author mboonk
 * 
 */
public class IfdConnectorImpl implements IfdConnector {

	private List<PcscListener> listeners = new LinkedList<PcscListener>();
	private List<VirtualReaderUi> userInterfaces = new LinkedList<VirtualReaderUi>();
	private IfdComm communication;
	private int timeout = 5000;

	@Override
	public void connect(IfdComm comm) throws IOException {
		communication = comm;
		communication.setListeners(listeners);
		communication.start();

		long timeOutTime = Calendar.getInstance().getTimeInMillis() + timeout;
		while (!communication.isRunning()){
			if (Calendar.getInstance().getTimeInMillis() > timeOutTime){
				throw new IOException("The communication thread has run into a timeout");
			}
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				throw new IOException("The waiting for the communication thread was interrupted");
			}
		}
	}

	@Override
	public boolean isRunning() {
		return ((communication != null) && communication.isRunning());
	}

	@Override
	public void disconnect() throws IOException, InterruptedException {
		if (communication != null){
			communication.stop();
		}
	}

	@Override
	public void addListener(PcscListener listener) {
		listeners.add(0, listener);
		if (listener instanceof UiEnabled){
			((UiEnabled)listener).setUserInterfaces(userInterfaces);
		}
		if (listener instanceof ConnectorEnabled){
			((ConnectorEnabled)listener).setConnector(this);
		}
	}

	@Override
	public void addUi(VirtualReaderUi ui) {
		this.userInterfaces.add(ui);
	}

	@Override
	public Collection<VirtualReaderUi> getUserInterfaces(){
		return new HashSet<VirtualReaderUi>(userInterfaces);
	}

	@Override
	public List<PcscListener> getListeners() {
		return new LinkedList<PcscListener> (listeners);
	}

	@Override
	public void reconnect() {
		communication.stop();
		communication.start();
	}


}
