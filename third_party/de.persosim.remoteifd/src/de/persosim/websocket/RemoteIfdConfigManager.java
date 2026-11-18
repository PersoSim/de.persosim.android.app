package de.persosim.websocket;

import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.Map;

/**
 * Implementations of this interface supply configuration information for the SaK interface implementation.
 * @author boonk.martin
 *
 */
public interface RemoteIfdConfigManager {

	Certificate getHostCertificate();

	RSAPrivateKey getHostPrivateKey();

	Map<Certificate, String> getPairedCertificates();

	void addPairedCertificate(Certificate certificate);

	void deletePairedCertificate(Certificate certificate);

	void updateUdNameForCertificate(Certificate certificate, String udName);

	String getName();

}