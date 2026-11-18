package de.persosim.websocket;

import de.persosim.driver.connector.IfdComm;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.simulator.utils.Base64;
import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.cert.CertificateEncodingException;
import java.util.List;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

public class WebsocketComm implements IfdComm, Runnable {
    private volatile boolean running;
    private final String pairingCode;
    private final RemoteIfdConfigManager configuration;
    private final HandshakeResultListener listener;
    private List<PcscListener> listeners;
    private Thread serverThread;
    private ServerSocket serverSocket;
    private Socket client;
    private Thread announcer;

    public WebsocketComm(String pairingCode, RemoteIfdConfigManager configuration,
            HandshakeResultListener listener) {
        this.pairingCode = pairingCode;
        this.configuration = configuration;
        this.listener = listener;
    }
    public WebsocketComm(String pairingCode, RemoteIfdConfigManager configuration) {
        this(pairingCode, configuration, null);
    }
    @Override public synchronized void start() {
        if (serverThread != null && serverThread.isAlive()) {
            if (running) return;
            throw new IllegalStateException("Previous reader session is still stopping");
        }
        try { serverSocket = new ServerSocket(0); }
        catch (IOException exception) { throw new IllegalStateException("Cannot start reader", exception); }
        running = true;
        serverThread = new Thread(this, "PersoSim-RemoteIFD");
        serverThread.start();
    }
    @Override public void stop() {
        Thread thread;
        synchronized (this) {
            running = false;
            thread = serverThread;
            if (announcer != null) announcer.interrupt();
            close(client);
            close(serverSocket);
            if (thread != null) thread.interrupt();
        }
        if (thread != null && thread != Thread.currentThread()) {
            try { thread.join(); }
            catch (InterruptedException exception) { Thread.currentThread().interrupt(); }
        }
    }
    @Override public boolean isRunning() { return running; }
    @Override public void setListeners(List<PcscListener> listeners) { this.listeners = listeners; }
    public synchronized int getPort() { return serverSocket == null ? -1 : serverSocket.getLocalPort(); }

    @Override public void run() {
        try {
            String id = "-----BEGIN CERTIFICATE-----\n"
                    + Base64.encode(configuration.getHostCertificate().getEncoded()).replaceAll("(.{64})", "$1\n")
                    + "-----END CERTIFICATE-----";
            ServerSocket server;
            synchronized (this) {
                if (!running) return;
                server = serverSocket;
            }
            while (running) {
                synchronized (this) {
                    if (!running) break;
                    announcer = new Thread(new Announcer(new DefaultAnnouncementMessageBuilder(
                            configuration.getName(), id, server.getLocalPort(), pairingCode != null)), "PersoSim-Announcer");
                    announcer.start();
                }
                try (Socket accepted = server.accept()) {
                    synchronized (this) {
                        if (!running) break;
                        client = accepted;
                        announcer.interrupt();
                        announcer = null;
                    }
                    accepted.setSoTimeout(90000);
                    TlsHandshaker handshaker = pairingCode == null
                            ? new DefaultHandshaker(configuration, accepted)
                            : new PairingServer(pairingCode, configuration, accepted);
                    boolean success = handshaker.performHandshake();
                    if (listener != null) listener.onHandshakeFinished(success);
                    if (success && running) {
                        try {
                            new WebSocketProtocol(handshaker.getInputStream(), handshaker.getOutputStream(),
                                    new DefaultMessageHandler(listeners, configuration, handshaker.getClientCertificate())).handleConnection();
                        } finally { handshaker.closeConnection(); }
                    }
                } catch (IOException | RuntimeException exception) {
                    if (running) BasicLogger.logException(getClass(), exception, LogLevel.WARN);
                } finally {
                    synchronized (this) { client = null; }
                    if (listener != null) listener.onConnectionClosed();
                }
                if (pairingCode != null) break;
            }
        } catch (CertificateEncodingException | RuntimeException exception) {
            if (running) BasicLogger.logException(getClass(), exception, LogLevel.WARN);
        } finally {
            synchronized (this) {
                running = false;
                if (announcer != null) announcer.interrupt();
                announcer = null;
                close(client);
                close(serverSocket);
                client = null;
                serverSocket = null;
            }
        }
    }
    private static void close(Closeable resource) {
        if (resource == null) return;
        try { resource.close(); }
        catch (IOException exception) { BasicLogger.logException(WebsocketComm.class, exception, LogLevel.DEBUG); }
    }
    @Override public void reset() { stop(); listeners = null; }
    @Override public String getName() { return "WEBSOCKET"; }
    @Override public String getUserString() { return "RemoteIFD interface"; }
}
