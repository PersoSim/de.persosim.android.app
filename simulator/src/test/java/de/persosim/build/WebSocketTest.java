package de.persosim.build;

import de.persosim.websocket.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class WebSocketTest {
    private static byte[] frame(String message) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(buffer);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        out.writeByte(0x81);
        if (bytes.length < 126) out.writeByte(0x80 | bytes.length);
        else { out.writeByte(0xfe); out.writeShort(bytes.length); }
        out.writeInt(0);
        out.write(bytes);
        return buffer.toByteArray();
    }
    private static final byte[] HEADER = ("GET / HTTP/1.1\r\nHost: localhost\r\n"
        + "Upgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==\r\n\r\n").getBytes(StandardCharsets.US_ASCII);
    private static void header(InputStream in) throws IOException {
        int matched = 0;
        byte[] end = new byte[] {13, 10, 13, 10};
        while (matched < end.length) {
            int value = in.read();
            if (value < 0) throw new EOFException();
            matched = value == end[matched] ? matched + 1 : 0;
        }
    }
    @Test public void coalescedHeaderAndLongMessageAreReadWithoutLosingBytes() throws Exception {
        de.persosim.simulator.utils.Base64.setEncoder(bytes -> java.util.Base64.getEncoder().encodeToString(bytes));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (ServerSocket server = new ServerSocket(0)) {
            Future<?> task = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    new WebSocketProtocol(socket.getInputStream(), socket.getOutputStream(), new MessageHandler() {
                        public String message(String value) { return value; }
                        public boolean isIccAvailable() { return false; }
                        public String getStatusMessage() { return null; }
                    }).handleConnection();
                } catch (IOException exception) { throw new UncheckedIOException(exception); }
            });
            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                client.setSoTimeout(5000);
                String large = new String(new char[40000]).replace('\0', 'x');
                ByteArrayOutputStream combined = new ByteArrayOutputStream();
                combined.write(HEADER);
                combined.write(frame(large));
                client.getOutputStream().write(combined.toByteArray());
                header(client.getInputStream());
                DataInputStream in = new DataInputStream(client.getInputStream());
                assertEquals(0x81, in.readUnsignedByte());
                assertEquals(126, in.readUnsignedByte());
                assertEquals(40000, in.readUnsignedShort());
                byte[] response = new byte[40000];
                in.readFully(response);
                assertEquals(large, new String(response, StandardCharsets.UTF_8));
                client.getOutputStream().write(new byte[] {(byte) 0x88, 2, 3, (byte) 0xe8});
                assertEquals(0x88, in.readUnsignedByte());
                assertEquals(2, in.readUnsignedByte());
                assertEquals(1000, in.readUnsignedShort());
            }
            task.get(5, TimeUnit.SECONDS);
        } finally { executor.shutdownNow(); }
    }
    @Test public void disconnectInterruptsPendingCommandBeforeReturning() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1), interrupted = new CountDownLatch(1);
        try (ServerSocket server = new ServerSocket(0)) {
            Future<?> task = executor.submit(() -> {
                try (Socket socket = server.accept()) {
                    new WebSocketProtocol(socket.getInputStream(), socket.getOutputStream(), new MessageHandler() {
                        public String message(String value) {
                            entered.countDown();
                            try { new CountDownLatch(1).await(); }
                            catch (InterruptedException exception) { interrupted.countDown(); Thread.currentThread().interrupt(); }
                            return null;
                        }
                        public boolean isIccAvailable() { return false; }
                        public String getStatusMessage() { return null; }
                    }).handleConnection();
                } catch (IOException exception) { throw new UncheckedIOException(exception); }
            });
            try (Socket client = new Socket("127.0.0.1", server.getLocalPort())) {
                client.setSoTimeout(5000);
                client.getOutputStream().write(HEADER);
                header(client.getInputStream());
                client.getOutputStream().write(frame("pending"));
                assertTrue(entered.await(5, TimeUnit.SECONDS));
            }
            task.get(5, TimeUnit.SECONDS);
            assertEquals(0, interrupted.getCount());
        } finally { executor.shutdownNow(); }
    }
}
