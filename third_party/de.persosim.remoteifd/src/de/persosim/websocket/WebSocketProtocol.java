package de.persosim.websocket;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.simulator.utils.Base64;
import de.persosim.simulator.utils.HexString;
import de.persosim.websocket.Frame.Opcode;

public class WebSocketProtocol {

	private DataInputStream inputStream;
	private DataOutputStream outputStream;
	private MessageHandler messageHandler;
	
	private Frame joinedFrame = null;
	private static final int MAX_FRAME_SIZE = 2 * 1024 * 1024;

	public WebSocketProtocol(InputStream inputStream, OutputStream outputStream, MessageHandler messageHandler) {
		this.inputStream = new DataInputStream(inputStream);
		this.outputStream = new DataOutputStream(outputStream);
		this.messageHandler = messageHandler;
	}

	public void handleConnection() {
		ConnectionState connectionState = ConnectionState.NEW;
		Thread iccPollingThread = null;

        java.util.concurrent.ExecutorService commands = new java.util.concurrent.ThreadPoolExecutor(1, 1, 0, java.util.concurrent.TimeUnit.MILLISECONDS,
                new java.util.concurrent.ArrayBlockingQueue<Runnable>(16));
        try {
		while (true) {
			switch (connectionState) {
			case NEW:
				if (handleHandshake()) {
					connectionState = ConnectionState.ESTABLISHED;
					iccPollingThread = new Thread(new Runnable() {
						
						private boolean lastIccState;

						@Override
						public void run() {
							while (!Thread.interrupted()) {
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									BasicLogger.logException(getClass(), "Sleeping of icc polling thread interrupted", e);
									Thread.currentThread().interrupt();
								}
								if (messageHandler.isIccAvailable() != lastIccState) {
									lastIccState = !lastIccState;
									String status = messageHandler.getStatusMessage();
                                    if (status != null) writeFrame(createBasicFrame(Opcode.TEXT, status.getBytes(StandardCharsets.UTF_8)));	
								}
							}
						}
					});
					iccPollingThread.start();
				} else return;
				
				break;
			case ESTABLISHED:
				Frame currentFrame = readFrame();
				
				if (currentFrame == null) {
					connectionState = ConnectionState.CLOSED;
					break;
				}
				
				if (currentFrame.getOpcode().isControl()) {
					BasicLogger.log(getClass(), "Handling control frame", LogLevel.TRACE);
					connectionState = handleFrame(currentFrame);
				} else {
					if (joinedFrame == null) {
						BasicLogger.log(getClass(), "Starting new joined frame", LogLevel.TRACE);
						joinedFrame = currentFrame;
					} else {
						BasicLogger.log(getClass(), "Appending to joined frame", LogLevel.TRACE);
						if ((long) joinedFrame.getPayload().length + currentFrame.getPayload().length > MAX_FRAME_SIZE)
                            throw new IllegalStateException("WebSocket message too large");
                        joinedFrame.appendFrame(currentFrame);
					}
					
					if (joinedFrame.getFin()) {
						BasicLogger.log(getClass(), "Joined frame complete, handling now", LogLevel.TRACE);
						final Frame frameToProcess = joinedFrame;
						joinedFrame = null;
                        commands.execute(() -> handleFrame(frameToProcess));
					}
				}
				
				break;
			case CLOSED:
				if (iccPollingThread != null) {
					iccPollingThread.interrupt();
				}
				return;
			default:
				break;
			}
		}
        } finally {
            commands.shutdownNow();
            if (iccPollingThread != null) iccPollingThread.interrupt();
            boolean interrupted = Thread.interrupted();
            while (!commands.isTerminated() || (iccPollingThread != null && iccPollingThread.isAlive())) {
                try {
                    commands.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS);
                    if (iccPollingThread != null) iccPollingThread.join(1000);
                } catch (InterruptedException exception) { interrupted = true; }
            }
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

	private boolean handleHandshake() {

		try {

			BasicLogger.log(getClass(), "Begin handling websocket handshake",
					LogLevel.DEBUG);
			
			String data = readToDelimiter(new char [] {'\r', '\n', '\r', '\n'});

			BasicLogger.log(getClass(), "Received message for websocket handshake: " + System.lineSeparator() + data,
					LogLevel.DEBUG);

			Matcher get = Pattern.compile("^GET").matcher(data);

			if (!get.find()) {
				BasicLogger.log(getClass(), "No GET found in handshake message");
				return false;
			}

			Matcher match = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);

			if (!match.find()) {

				BasicLogger.log(getClass(), "No Sec-WebSocket-Key found in handshake message");
				return false;
			}

			String challengeResponse = Base64.encode(MessageDigest.getInstance("SHA-1").digest(
					(match.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes(StandardCharsets.UTF_8)));
			String response = ("HTTP/1.1 101 Switching Protocols\r\n" + "Connection: Upgrade\r\n"
					+ "Upgrade: websocket\r\n" + "Sec-WebSocket-Accept: " + challengeResponse + "\r\n\r\n");

			BasicLogger.log(getClass(),
					"Sending response message for websocket handshake: " + System.lineSeparator() + response,
					LogLevel.DEBUG);
			byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
			outputStream.write(responseBytes, 0, responseBytes.length);
			outputStream.flush();
			return true;
		} catch (NoSuchAlgorithmException | IOException e) {
			BasicLogger.logException(getClass(), e);
			return false;
		}
	}

	private String readToDelimiter(char[] cs) {
				
		int indexToDelimiter = 0;
		StringBuilder data = new StringBuilder();
		
		do {
			try {
				int read = inputStream.readUnsignedByte();
				char readChar = (char) read;
				
				data.append(readChar);
                if (data.length() > 16384) throw new IllegalStateException("WebSocket header too large");
				
				if (read == -1) {
					throw new IllegalStateException("Reading reached EOF unexpectedly");
				}
				
				if (readChar == cs[indexToDelimiter]) {
					indexToDelimiter++;
				} else {
					indexToDelimiter = 0;
				}
				
			} catch (IOException e) {
				throw new IllegalStateException("Reading of handshake message failed with exception", e);
			}
			
		} while (indexToDelimiter != cs.length);
		
		return data.toString();
	}

	private ConnectionState handleFrame(Frame curFrame) {
		switch (curFrame.getOpcode()) {
		case CLOSE:
			// Echo first to bytes on close (status code, see RFC6455 5.5.1)
			if (curFrame.getPayload().length >= 2) {
				writeFrame(createBasicFrame(Opcode.CLOSE,
						new byte[] { curFrame.getPayload()[0], curFrame.getPayload()[1] }));

			} else {
				writeFrame(createBasicFrame(Opcode.CLOSE, new byte[0]));
			}
			return ConnectionState.CLOSED;
		case PING:
			writeFrame(createBasicFrame(Opcode.PONG, curFrame.getPayload()));
			return ConnectionState.ESTABLISHED;
		case TEXT:
			Frame responseFrame = handleTextFrame(curFrame);
			if (responseFrame != null) {
				writeFrame(responseFrame);	
			}
			
			return ConnectionState.ESTABLISHED;
		default:
			BasicLogger.log(getClass(), "Got message with unhandled opcode: " + curFrame.toString(), LogLevel.WARN);
			writeFrame(createBasicFrame(Opcode.CLOSE));
			return ConnectionState.CLOSED;
		}
	}

	private Frame createBasicFrame(Opcode opcode, byte[] payload) {
		Frame result = createBasicFrame(opcode);
		result.setPayload(payload);
		return result;
	}

	private Frame createBasicFrame(Opcode opcode) {
		Frame result = new Frame();
		result.setFin(true);
		result.setOpcode(opcode);
		return result;
	}

	private Frame handleTextFrame(Frame frame) {
		String message = messageHandler
				.message(new String(frame.getPayload(), StandardCharsets.UTF_8));

		if (message != null) {
			return createBasicFrame(Opcode.TEXT, message.getBytes(StandardCharsets.UTF_8));
		}
		return null;
	}

	private synchronized void writeFrame(Frame frame) {

		short header = 0;

		header |= frame.getFin() ? 0x8000 : 0;
		// IMPL set RSV
		header |= (frame.getOpcode().getValue() << 8);

		if (frame.getPayload().length <= 125) {
			header |= frame.getPayload().length;
		} else if (frame.getPayload().length <= 65535) {
			header |= 126;
		} else {
			header |= 127;
		}

		StringBuilder logMessage = new StringBuilder();
		ByteBuffer message = ByteBuffer.allocate(frame.getPayload().length + 10);

		try {
			message.putShort(header);

			if (frame.getPayload().length <= 65535 && frame.getPayload().length > 125) {
				message.putShort((short) (0xFFFF & frame.getPayload().length));
			} else if (frame.getPayload().length > 65535) {
				message.putLong(frame.getPayload().length);
			}

			message.put(frame.getPayload());

			message.flip();
			byte[] toWrite = new byte[message.remaining()];
			message.get(toWrite);
			outputStream.write(toWrite);
            outputStream.flush();

			logMessage.append("Sent frame: " + HexString.encode(toWrite));
			BasicLogger.log(getClass(), logMessage.toString(), LogLevel.TRACE);

		} catch (IOException e) {
			BasicLogger.logException(getClass(),  "Writing a frame failed", e);
		}
	}

	private Frame readFrame() {
		try {
			Frame result = new Frame();

			int header = inputStream.readUnsignedShort();

			boolean fin = (header & 0x8000) == 0x8000;
			boolean rsv1 = (header & 0x4000) == 0x4000;
			boolean rsv2 = (header & 0x2000) == 0x2000;
			boolean rsv3 = (header & 0x1000) == 0x1000;
			int opcode = (header & 0xF00) >>> 8;

			boolean mask = (header & 0x80) == 0x80;

			long payloadLength = (header & 0x7F);

			if (payloadLength == 126) {
				payloadLength = inputStream.readUnsignedShort();
			} else if (payloadLength == 127) {
				payloadLength = inputStream.readLong();
			}

            if (payloadLength < 0 || payloadLength > MAX_FRAME_SIZE) throw new IOException("WebSocket frame too large");
			byte[] maskingKey = new byte[4];

			if (mask) {
				inputStream.readFully(maskingKey);
			}

			StringBuilder logMessage = new StringBuilder();
			logMessage.append("Received Frame Header:" + System.lineSeparator() + "FIN: " + fin
					+ System.lineSeparator() + "Opcode: " + Opcode.forValue(opcode).toString() + System.lineSeparator() + "Mask: " + mask
					+ System.lineSeparator() + "Payload Length: " + payloadLength);
			BasicLogger.log(getClass(), logMessage.toString(), LogLevel.DEBUG);

			// IMPL handle extension data

			int readBytes = 0;

			ByteArrayOutputStream payload = new ByteArrayOutputStream();

			while (readBytes < payloadLength) {
				byte current = inputStream.readByte();
				if (mask) {
					current = (byte) (current ^ maskingKey[readBytes % 4]);
				}
				readBytes++;
				payload.write(current);
			}
			
			logMessage = new StringBuilder();

			logMessage.append("Received message:" + System.lineSeparator());

			for (byte b : payload.toByteArray()) {
				logMessage.append(Integer.toHexString((byte) (0xFF & b)));
			}

			BasicLogger.log(getClass(), logMessage.toString(), LogLevel.TRACE);

			result.setFin(fin);
			result.setRSV1(rsv1);
			result.setRSV2(rsv2);
			result.setRSV3(rsv3);
			result.setOpcode(Opcode.forValue(opcode));
			result.setPayload(payload.toByteArray());

			return result;
		} catch (IOException e) {
			BasicLogger.logException(getClass(), "Reading and parsing a new frame failed", e);
		}
		return null;
	}

}
