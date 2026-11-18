package de.persosim.websocket;

import java.io.InputStream;
import java.io.OutputStream;

import org.bouncycastle.tls.Certificate;

public interface TlsHandshaker {

	boolean performHandshake();

	void closeConnection();

	InputStream getInputStream();

	OutputStream getOutputStream();
	
	Certificate getClientCertificate();

}
