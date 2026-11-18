package de.persosim.websocket;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPrivateKey;

import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.tls.crypto.TlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCertificate;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;

/**
 * Converts certificates and keys from Java classes to Bouncycastle and back. 
 * @author boonk.martin
 *
 */
public class CertificateConverter {

	public static Certificate fromBcTlsCertificateToJavaCertificate(org.bouncycastle.tls.Certificate cert) {
		byte[] encoded;
		try {
			encoded = cert.getCertificateList()[0].getEncoded();
			java.security.cert.Certificate jCert = CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(encoded));
			return jCert;
		} catch (IOException | CertificateException e) {
			throw new IllegalStateException("Could not convert certificate " + cert, e);
		}
	}

	public static AsymmetricKeyParameter fromJavaKeyToBcAsymetricKeyParameter(RSAPrivateKey hostPrivateKey) {
		return new RSAKeyParameters(true, hostPrivateKey.getModulus(),
				hostPrivateKey.getPrivateExponent());
	}

	public static org.bouncycastle.tls.Certificate fromJavaCertificateToBcTlsCertificate(Certificate cert) {
		TlsCertificate[] certs = new TlsCertificate[1];
		
		try {
			certs[0] = new BcTlsCertificate(new BcTlsCrypto(new SecureRandom()), cert.getEncoded());
			return new org.bouncycastle.tls.Certificate(certs);
		} catch (CertificateEncodingException | IOException e) {
			throw new IllegalStateException("Could not convert certificate " + cert, e);
		}

	}

}
