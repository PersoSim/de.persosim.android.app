package de.persosim.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.SecureRandom;
import java.util.Vector;

import org.bouncycastle.tls.Certificate;
import org.bouncycastle.tls.CertificateRequest;
import org.bouncycastle.tls.ClientCertificateType;
import org.bouncycastle.tls.DefaultTlsServer;
import org.bouncycastle.tls.HashAlgorithm;
import org.bouncycastle.tls.SignatureAlgorithm;
import org.bouncycastle.tls.SignatureAndHashAlgorithm;
import org.bouncycastle.tls.TlsCredentialedSigner;
import org.bouncycastle.tls.TlsServerProtocol;
import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.tls.crypto.TlsCryptoParameters;
import org.bouncycastle.tls.crypto.impl.bc.BcDefaultTlsCredentialedSigner;
import org.bouncycastle.tls.crypto.impl.bc.BcTlsCrypto;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.simulator.utils.HexString;

public class DefaultHandshaker implements TlsHandshaker {

	private Socket clientSocket;
	private TlsServerProtocol protocol;
	private RemoteIfdConfigManager remoteIfdConfig;
	protected Certificate clientCert;

	public DefaultHandshaker(RemoteIfdConfigManager remoteIfdConfig, Socket client) {
		this.clientSocket = client;
		this.remoteIfdConfig = remoteIfdConfig;
	}

	@Override
	public boolean performHandshake() {
		TlsCrypto crypto = new BcTlsCrypto(new SecureRandom());

		try {

			protocol = new TlsServerProtocol(clientSocket.getInputStream(), clientSocket.getOutputStream());

			protocol.accept(new DefaultTlsServer(crypto) {

                @Override protected org.bouncycastle.tls.ProtocolVersion[] getSupportedVersions() {
                    return org.bouncycastle.tls.ProtocolVersion.TLSv12.only();
                }

                @Override
                protected TlsCredentialedSigner getRSASignerCredentials() throws IOException {
					return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context),
							(BcTlsCrypto) getCrypto(),
							CertificateConverter.fromJavaKeyToBcAsymetricKeyParameter(remoteIfdConfig.getHostPrivateKey()),
							CertificateConverter.fromJavaCertificateToBcTlsCertificate(remoteIfdConfig.getHostCertificate()),
							new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.rsa));
				}

				public void notifyClientCertificate(Certificate arg0) throws IOException {
					validateCertificate(arg0);
					clientCert = arg0;
				};

				private void validateCertificate(Certificate cert) {
					java.security.cert.Certificate javaCert = CertificateConverter.fromBcTlsCertificateToJavaCertificate(cert);
					if (!remoteIfdConfig.getPairedCertificates().keySet().contains(javaCert)) {
						BasicLogger.log(getClass(), "The certificate with serial 0x" + HexString.encode(cert.getCertificateAt(0).getSerialNumber()) + " is not paired");
						throw new IllegalArgumentException("Unknown cert " + cert);
					}
				}

				public CertificateRequest getCertificateRequest() {
					Vector<Object> signatureAndHash = new Vector<>();
					signatureAndHash.add(new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.rsa));
					return new CertificateRequest(new short[] { ClientCertificateType.rsa_sign }, signatureAndHash, null);
				}

				@Override
				public void notifyHandshakeComplete() throws IOException {
					super.notifyHandshakeComplete();
					BasicLogger.log(getClass(), "Handshake done", LogLevel.DEBUG);
				}

				@Override
				public void notifyOfferedCipherSuites(int[] arg0) throws IOException {
					super.notifyOfferedCipherSuites(arg0);
					String cipherSuites = "Offered cipher suites:";
					for (int i : arg0) {
						cipherSuites += System.lineSeparator() + Integer.toHexString(i);
					}
					BasicLogger.log(getClass(), cipherSuites, LogLevel.DEBUG);
				}

			});
			return true;

		} catch (IOException e) {
			BasicLogger.logException(getClass(), "Other side closed the connection", e, LogLevel.WARN);
		}
		return false;
	}

	@Override
	public void closeConnection() {
		try {
			BasicLogger.log(getClass(), "Closing TLS connection", LogLevel.DEBUG);
			protocol.close();
		} catch (IOException e) {
			// Expected when peer closes socket too early
			BasicLogger.logException(getClass(), "Exception during closing of tls server, probably due to early close",
					e, LogLevel.INFO);
		}
	}

	@Override
	public InputStream getInputStream() {
		return protocol.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() {
		return protocol.getOutputStream();
	}

	@Override
	public Certificate getClientCertificate() {
		return clientCert;
	}

}
