package de.persosim.build;

import de.persosim.websocket.*;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.*;
import java.util.concurrent.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.jcajce.*;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.tls.*;
import org.bouncycastle.tls.crypto.*;
import org.bouncycastle.tls.crypto.impl.bc.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class RemoteReaderTest {
    static final class Identity implements RemoteIfdConfigManager {
        final KeyPair keys;
        final Certificate cert;
        final Map<Certificate, String> paired = new ConcurrentHashMap<>();
        Identity() throws Exception {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            keys = gen.generateKeyPair();
            X500Name name = new X500Name("CN=Test reader");
            org.bouncycastle.jce.provider.BouncyCastleProvider provider = new org.bouncycastle.jce.provider.BouncyCastleProvider();
            cert = new JcaX509CertificateConverter().setProvider(provider).getCertificate(
                new JcaX509v3CertificateBuilder(name, BigInteger.ONE, new Date(0), new Date(4102444800000L), name, keys.getPublic())
                    .build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(keys.getPrivate())));
        }
        public Certificate getHostCertificate() { return cert; }
        public RSAPrivateKey getHostPrivateKey() { return (RSAPrivateKey) keys.getPrivate(); }
        public String getName() { return "Test reader"; }
        public Map<Certificate, String> getPairedCertificates() { return paired; }
        public void addPairedCertificate(Certificate cert) { paired.put(cert, "Test"); }
        public void deletePairedCertificate(Certificate cert) { paired.remove(cert); }
        public void updateUdNameForCertificate(Certificate cert, String name) { paired.replace(cert, name); }
    }

    static TlsAuthentication authentication(Identity client, TlsContext context, BcTlsCrypto crypto) {
        return new TlsAuthentication() {
            public void notifyServerCertificate(TlsServerCertificate cert) { }
            public TlsCredentials getClientCredentials(CertificateRequest request) {
                return new BcDefaultTlsCredentialedSigner(new TlsCryptoParameters(context), crypto,
                    CertificateConverter.fromJavaKeyToBcAsymetricKeyParameter(client.getHostPrivateKey()),
                    CertificateConverter.fromJavaCertificateToBcTlsCertificate(client.cert),
                    new SignatureAndHashAlgorithm(HashAlgorithm.sha256, SignatureAlgorithm.rsa));
            }
        };
    }

    static boolean handshake(Identity host, Identity client, String serverCode, String clientCode) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0)) {
            Future<Boolean> accepted = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    socket.setSoTimeout(5000);
                    TlsHandshaker handshaker = serverCode == null ? new DefaultHandshaker(host, socket) : new PairingServer(serverCode, host, socket);
                    try { return handshaker.performHandshake(); }
                    catch (RuntimeException exception) { return false; }
                }
            });
            boolean connected = false;
            try (Socket socket = new Socket("127.0.0.1", server.getLocalPort())) {
                socket.setSoTimeout(5000);
                BcTlsCrypto crypto = new BcTlsCrypto(new SecureRandom());
                TlsClientProtocol protocol = new TlsClientProtocol(socket.getInputStream(), socket.getOutputStream());
                TlsClient tlsClient;
                if (clientCode != null) {
                    tlsClient = new PSKTlsClient(crypto, new byte[0], clientCode.getBytes(java.nio.charset.StandardCharsets.US_ASCII)) {
                        protected int[] getSupportedCipherSuites() { return new int[] {CipherSuite.TLS_RSA_PSK_WITH_AES_256_CBC_SHA}; }
                        public TlsAuthentication getAuthentication() { return authentication(client, context, crypto); }
                        public TlsKeyExchangeFactory getKeyExchangeFactory() {
                            return new DefaultTlsKeyExchangeFactory() {
                                public TlsKeyExchange createPSKKeyExchangeClient(int algorithm, TlsPSKIdentity identity, TlsDHGroupVerifier verifier) {
                                    return new TlsPSKKeyExchange(algorithm, identity, verifier) {
                                        public short[] getClientCertificateTypes() { return new short[] {ClientCertificateType.rsa_sign}; }
                                        public void processClientCredentials(TlsCredentials credentials) { }
                                    };
                                }
                            };
                        }
                    };
                } else {
                    tlsClient = new DefaultTlsClient(crypto) {
                        protected ProtocolVersion[] getSupportedVersions() { return ProtocolVersion.TLSv12.only(); }
                        public TlsAuthentication getAuthentication() { return authentication(client, context, crypto); }
                    };
                }
                try { protocol.connect(tlsClient); connected = true; }
                catch (IOException exception) { connected = false; }
            }
            return accepted.get(7, TimeUnit.SECONDS) && connected;
        } finally { executor.shutdownNow(); }
    }

    @Test public void pairingPersistsCertificateAndRejectsWrongCodeAndUnknownClient() throws Exception {
        Identity host = new Identity(), client = new Identity();
        assertFalse(handshake(host, client, "1234", "4321"));
        assertTrue(host.paired.isEmpty());
        assertTrue(handshake(host, client, "1234", "1234"));
        assertTrue(host.paired.containsKey(client.cert));
        assertTrue(handshake(host, client, null, null));
        assertFalse(handshake(host, new Identity(), null, null));
    }

    @Test public void stopClosesStalledHandshakeAndSupportsRestart() throws Exception {
        de.persosim.simulator.utils.Base64.setEncoder(bytes -> java.util.Base64.getEncoder().encodeToString(bytes));
        WebsocketComm server = new WebsocketComm(null, new Identity());
        server.setListeners(Collections.emptyList());
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            server.start();
            assertTrue(server.getPort() > 0);
            try (Socket stalled = new Socket("127.0.0.1", server.getPort())) {
                executor.submit(server::stop).get(5, TimeUnit.SECONDS);
                assertFalse(server.isRunning());
            }
            server.start();
            assertTrue(server.isRunning());
        } finally { server.stop(); executor.shutdownNow(); }
    }
}
