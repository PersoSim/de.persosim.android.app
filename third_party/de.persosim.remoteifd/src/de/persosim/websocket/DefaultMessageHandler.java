package de.persosim.websocket;

import static de.persosim.websocket.IfdProtocolWebSocket.CONTEXT_HANDLE;
import static de.persosim.websocket.IfdProtocolWebSocket.IFD_ERROR;
import static de.persosim.websocket.IfdProtocolWebSocket.IFD_ESTABLISH_CONTEXT;
import static de.persosim.websocket.IfdProtocolWebSocket.MSG;
import static de.persosim.websocket.IfdProtocolWebSocket.PROTOCOL;
import static de.persosim.websocket.IfdProtocolWebSocket.RESULT_MAJOR;
import static de.persosim.websocket.IfdProtocolWebSocket.RESULT_MINOR;
import static de.persosim.websocket.IfdProtocolWebSocket.SLOT_HANDLE;
import static de.persosim.websocket.IfdProtocolWebSocket.UD_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.tls.Certificate;
import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.json.JSONObject;

import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.features.ModifyPinDirect;
import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.simulator.apdu.CommandApdu;
import de.persosim.simulator.apdu.CommandApduFactory;
import de.persosim.simulator.tlv.ConstructedTlvDataObject;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvConstants;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;
import de.persosim.websocket.IfdProtocolWebSocket.ContextProvider;

public class DefaultMessageHandler implements MessageHandler, ContextProvider {
	
	public static HashMap<String, IfdProtocolWebSocket> supportedProtocols = new HashMap<>();
	static {
		supportedProtocols.put(IfdProtocolWebSocketV0.IDENTIFIER, new IfdProtocolWebSocketV0());
		supportedProtocols.put(IfdProtocolWebSocketV2.IDENTIFIER, new IfdProtocolWebSocketV2());
	}
	
	public static List<String> getSupportedApi() {
		ArrayList<String> retVal = new ArrayList<>();
		retVal.addAll(supportedProtocols.keySet());
		return retVal;
	}	
	
	private static final byte CCID_FUNCTION_GET_READER_PACE_CAPABITILIES = 1;
	private static final byte CCID_FUNCTION_DESTROY_PACE_CHANNEL = 3;
	private static final byte CCID_FUNCTION_ESTABLISH_PACE_CHANNEL = 2;
	
	String contextHandle = "PersoSimContextHandle";
	private List<PcscListener> listeners;
	UnsignedInteger lun = new UnsignedInteger(1);
	
	private RemoteIfdConfigManager remoteIfdConfig;
	private Certificate clientCertificate;
	private volatile IfdProtocolWebSocket currentProtocol;

	public DefaultMessageHandler(List<PcscListener> listeners, RemoteIfdConfigManager remoteIfdConfig, Certificate clientCertificate) {
		this.listeners = listeners;
		this.remoteIfdConfig = remoteIfdConfig;
		this.clientCertificate = clientCertificate;
	}

	@Override
	public String message(String incomingMessage) {
		BasicLogger.log(getClass(), "Received JSON message: " + System.lineSeparator() + incomingMessage,
				LogLevel.TRACE);

		JSONObject jsonMessage = new JSONObject(incomingMessage);

		String messageType = jsonMessage.getString(MSG);

		String incomingContextHandle = null;
		if (jsonMessage.has(CONTEXT_HANDLE)) {
			incomingContextHandle = jsonMessage.getString(CONTEXT_HANDLE);
		}

		String incomingSlotHandle = null;
		if (jsonMessage.has(SLOT_HANDLE)) {
			incomingSlotHandle = jsonMessage.getString(SLOT_HANDLE);
		}
		
		BasicLogger.log(getClass(), "Received Json message with type: " + messageType + ", ContextHandle: " + incomingContextHandle + ", SlotHandle: " + incomingSlotHandle, LogLevel.TRACE);

		JSONObject response = new JSONObject();
		switch (messageType) {
		case IFD_ERROR:
			return null;
		case IFD_ESTABLISH_CONTEXT:
			if (jsonMessage.has(UD_NAME)) {
				remoteIfdConfig.updateUdNameForCertificate(CertificateConverter.fromBcTlsCertificateToJavaCertificate(clientCertificate), jsonMessage.getString(UD_NAME));
			}
			
			if (jsonMessage.has(PROTOCOL)) {
				currentProtocol = supportedProtocols.get(jsonMessage.getString(PROTOCOL));
			} else {
				currentProtocol = null;
			}
			//fallthrough: ESTABLISH_CONTEXT and all following message types are handled by selected protocol
		default:
			if (currentProtocol == null) {
				setErrorResult(response, Tr03112codes.TERMINAL_RESULT_COMMON_UNSUPPORTED_PROTOCOL);
				break;
			}

			response = currentProtocol.message(jsonMessage, this);
		}

		BasicLogger.log(getClass(), "Send JSON message: " + System.lineSeparator() + response.toString(),
				LogLevel.TRACE);
		return response.toString();
	}

	private void setErrorResult(JSONObject response, String resultMinor) {
		response.put(RESULT_MAJOR, Tr03112codes.RESULT_MAJOR_ERROR);
		response.put(RESULT_MINOR, resultMinor);
	}

	public byte[] pcscPerformEstablishPaceChannel(byte[] ccidMappedApdu) {
		CommandApdu apdu = CommandApduFactory.createCommandApdu(ccidMappedApdu);

		UnsignedInteger controlCode = pcscPerformGetFeatures().get(PersoSimPcscProcessor.FEATURE_CONTROL_CODE);


		if (apdu.getP2() != 2) {
			throw new IllegalArgumentException("Unexpected CCID P2 value of " + HexString.hexifyByte(apdu.getP2()));
		}
		
		List<byte[]> parameters = new LinkedList<>();

		parameters.add(controlCode.getAsByteArray());
		
		if (!apdu.getCommandData().isEmpty()) {
			byte[] convertCcidToPcscInputBuffer = convertCcidToPcscInputBuffer(apdu);
			if (convertCcidToPcscInputBuffer == null) {
				return null;
			}
			parameters.add(convertCcidToPcscInputBuffer);
		}
		
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));

		PcscCallResult result = doPcsc(new PcscCallData(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL, lun, parameters));
		
		if (PcscConstants.IFD_SUCCESS.equals(result.getResponseCode())) {
			UnsignedInteger paceStatusCode = new UnsignedInteger(Arrays.copyOfRange(result.getData().get(0), 0, 4));
			if (result.getData().get(0).length > 6) {
				return convertPcscToCcidOutputBuffer(paceStatusCode,
						Arrays.copyOfRange(result.getData().get(0), 6, result.getData().get(0).length));
			} else {
				return null;
			}
			
		}
		throw new IllegalStateException("Call for performing establish pace channel was not successful: "
				+ result.getResponseCode().getAsHexString());
	}

	public byte [] pcscPerformModifyPin(byte[] byteArray) {
		byte [] failureResult = Utils.toUnsignedByteArray(0x6F00);
		
		if (byteArray.length < 4) {
			return failureResult;
		}
		
		UnsignedInteger controlCode = pcscPerformGetFeatures().get(ModifyPinDirect.FEATURE_TAG);

		List<byte[]> parameters = new LinkedList<>();
		parameters.add(controlCode.getAsByteArray());
		
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));
		
		PcscCallResult result = doPcsc(new PcscCallData(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL, lun, parameters));
		
		return result.getData().get(0);
	}

	private byte[] convertPcscToCcidOutputBuffer(UnsignedInteger errorCode, byte[] pcscOutputBuffer) {
		int offset = 2;
		short status = Utils.getShortFromUnsignedByteArray(Arrays.copyOfRange(pcscOutputBuffer, 0, offset));
		byte [] cardAccess = Utils.getValueFlippedByteOrder(pcscOutputBuffer, offset, 2);
		offset += cardAccess.length + 2;
		byte [] carCurrent = Utils.getValueFlippedByteOrder(pcscOutputBuffer, offset, 1);
		offset += carCurrent.length + 1;
		byte [] carPrevious = Utils.getValueFlippedByteOrder(pcscOutputBuffer, offset, 1);
		offset += carPrevious.length + 1;
		byte [] idicc = Utils.getValueFlippedByteOrder(pcscOutputBuffer, offset, 2);

		
		TlvDataObject errorCodeTlv = new ConstructedTlvDataObject(TlvConstants.TAG_A1, new PrimitiveTlvDataObject(TlvConstants.TAG_OCTET_STRING, errorCode.getAsByteArray()));
		TlvDataObject statusTlv = new ConstructedTlvDataObject(TlvConstants.TAG_A2, new PrimitiveTlvDataObject(TlvConstants.TAG_OCTET_STRING, Utils.toUnsignedByteArray(status)));
		TlvDataObject cardAccessTlv = new ConstructedTlvDataObject(TlvConstants.TAG_A3, new ConstructedTlvDataObject(cardAccess));
		ConstructedTlvDataObject sequence = new ConstructedTlvDataObject(TlvConstants.TAG_SEQUENCE, errorCodeTlv, statusTlv, cardAccessTlv);
		
		if (idicc.length > 0) {
			sequence.addTlvDataObject(new ConstructedTlvDataObject(TlvConstants.TAG_A4, new PrimitiveTlvDataObject(TlvConstants.TAG_OCTET_STRING, idicc)));
		}		
		if (carCurrent.length > 0) {
			sequence.addTlvDataObject(new ConstructedTlvDataObject(TlvConstants.TAG_A5, new PrimitiveTlvDataObject(TlvConstants.TAG_OCTET_STRING, carCurrent)));
		}		
		if (carPrevious.length > 0) {
			sequence.addTlvDataObject(new ConstructedTlvDataObject(TlvConstants.TAG_A6, new PrimitiveTlvDataObject(TlvConstants.TAG_OCTET_STRING, carPrevious)));
		}
		
		return Utils.appendBytes(sequence.toByteArray(), (byte) 0x90, (byte)0x00);
	}

	private byte[] convertCcidToPcscInputBuffer(CommandApdu apdu) {
		byte function = -1;
		
		switch (apdu.getP2()) {
		case CCID_FUNCTION_GET_READER_PACE_CAPABITILIES:
			function = PersoSimPcscProcessor.FUNCTION_GET_READER_PACE_CAPABILITIES;
			break;
		case CCID_FUNCTION_DESTROY_PACE_CHANNEL:
			function = PersoSimPcscProcessor.FUNCTION_DESTROY_PACE_CHANNEL;
			break;
		case CCID_FUNCTION_ESTABLISH_PACE_CHANNEL:
			function = PersoSimPcscProcessor.FUNCTION_ESTABLISH_PACE_CHANNEL;
			break;
		default:
			return null;
		}
		
		TlvDataObjectContainer inputData = apdu.getCommandDataObjectContainer();
		
		ConstructedTlvDataObject sequence = (ConstructedTlvDataObject) inputData.getTlvDataObject(TlvConstants.TAG_SEQUENCE);
		byte passwordId = ((ConstructedTlvDataObject)sequence.getTlvDataObject(TlvConstants.TAG_A1)).getTlvDataObject(TlvConstants.TAG_INTEGER).getValueField()[0];
		byte [] transmittedPassword = sequence.containsTlvDataObject(TlvConstants.TAG_A2) ? ((ConstructedTlvDataObject)sequence.getTlvDataObject(TlvConstants.TAG_A2)).getTlvDataObject(TlvConstants.TAG_NUMERIC_STRING).getValueField() : null;
		byte [] chat = sequence.containsTlvDataObject(TlvConstants.TAG_A3) ? ((ConstructedTlvDataObject)sequence.getTlvDataObject(TlvConstants.TAG_A3)).getTlvDataObject(TlvConstants.TAG_OCTET_STRING).getValueField() : null;
		byte [] certificateDescription = sequence.containsTlvDataObject(TlvConstants.TAG_A4) ? ((ConstructedTlvDataObject)sequence.getTlvDataObject(TlvConstants.TAG_A4)).getTlvDataObject(TlvConstants.TAG_SEQUENCE).getValueField() : null;
		byte [] hashOid = sequence.containsTlvDataObject(TlvConstants.TAG_A5) ? ((ConstructedTlvDataObject)sequence.getTlvDataObject(TlvConstants.TAG_A5)).getTlvDataObject(TlvConstants.TAG_OID).getValueField() : null;
		
		byte [] pcscInputData = new byte [] { passwordId };
		if (chat != null) {
			pcscInputData = Utils.concatByteArrays(pcscInputData, Utils.createLengthValueFlippedByteOrder(chat, 1));
		} else {
			pcscInputData = Utils.appendBytes(pcscInputData, (byte) 0);
		}
		if (transmittedPassword != null) {
			pcscInputData = Utils.concatByteArrays(pcscInputData, Utils.createLengthValueFlippedByteOrder(transmittedPassword, 1));
		} else {
			pcscInputData = Utils.appendBytes(pcscInputData, (byte) 0);
		}
		if (certificateDescription != null) {
			pcscInputData = Utils.concatByteArrays(pcscInputData, Utils.createLengthValueFlippedByteOrder(certificateDescription, 2));
		} else {
			pcscInputData = Utils.appendBytes(pcscInputData, (byte) 0, (byte) 0);
		}
		if (hashOid != null) {
			pcscInputData = Utils.concatByteArrays(pcscInputData, Utils.createLengthValueFlippedByteOrder(hashOid, 2));
		} else {
			pcscInputData = Utils.appendBytes(pcscInputData, (byte) 0, (byte) 0);
		}
		

		byte [] pcscFormattedInputData = new byte [] { function };
		pcscFormattedInputData = Utils.concatByteArrays(pcscFormattedInputData, Utils.createLengthValueFlippedByteOrder(pcscInputData, 2));
		return pcscFormattedInputData;
	}
	
	public Map<Byte, UnsignedInteger> pcscPerformGetFeatures(){
		List<byte[]> parameters = new LinkedList<>();

		parameters.add(PcscConstants.CONTROL_CODE_GET_FEATURE_REQUEST.getAsByteArray());

		parameters.add(new byte [0]);
		
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));
		
		PcscCallResult result = doPcsc(new PcscCallData(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL, lun, parameters));
		
		if (PcscConstants.IFD_SUCCESS.equals(result.getResponseCode())) {
			Map<Byte, UnsignedInteger> defs = new HashMap<>();
			if (!result.getData().isEmpty()) {
				byte[] features = result.getData().get(0);

				for (int i = 0; i < features.length; i += 6) {
					defs.put(features[i], new UnsignedInteger(Arrays.copyOfRange(features, i + 2, i + 6)));
				}

			}
			return defs;
		}

		throw new IllegalStateException("Call for performing get features was not successful: "
				+ result.getResponseCode().getAsHexString());
	}

	public byte[] pcscPerformGetReaderPaceCapabilities() {

		UnsignedInteger controlCode = pcscPerformGetFeatures().get(PersoSimPcscProcessor.FEATURE_CONTROL_CODE);
		
		if (controlCode == null) {
			//no PACE feature
			return new byte [] {1,0};
			
		}

		List<byte[]> parameters = new LinkedList<>();

		parameters.add(controlCode.getAsByteArray());

		parameters.add(new byte [] {PersoSimPcscProcessor.FUNCTION_GET_READER_PACE_CAPABILITIES});
		
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));

		PcscCallResult result = doPcsc(new PcscCallData(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL, lun, parameters));

		if (PcscConstants.IFD_SUCCESS.equals(result.getResponseCode()) && PersoSimPcscProcessor.RESULT_NO_ERROR
				.equals(new UnsignedInteger(Arrays.copyOfRange(result.getData().get(0), 0, 4))) && !result.getData().isEmpty()) {
			return Arrays.copyOfRange(result.getData().get(0), 6, 8);
		}
		
		throw new IllegalStateException("Call for performing get reader pace capabilities was not successful: "
				+ result.getResponseCode().getAsHexString());
	}

	
	public PcscCallResult doPcsc(UnsignedInteger function, List<byte[]> parameters) {
		return doPcsc(new PcscCallData(function, lun, parameters));
	}
	
	private PcscCallResult doPcsc(PcscCallData callData) {
		PcscCallResult result = null;
		if (listeners != null) {
			for (PcscListener listener : listeners) {
				try {
					PcscCallResult currentResult = listener.processPcscCall(callData);
					if (result == null && currentResult != null) {
						// ignore all but the first result
						result = currentResult;
					}
				} catch (RuntimeException e) {
					BasicLogger.logException(getClass(),
							"Something went wrong while processing of the PCSC data by listener \""
									+ listener.getClass().getName() + "\"!\"",
							e, LogLevel.ERROR);
				}
			}
		} else {
			BasicLogger.log(getClass(), "No PCSC listeners registered!", LogLevel.WARN);
		}
		if (result == null) {
			result = new SimplePcscCallResult(PcscConstants.IFD_NOT_SUPPORTED);
		}
		return result;
	}

	public boolean pcscPowerIcc(UnsignedInteger pcscPowerFunction) {
		List<byte[]> parameters = new LinkedList<>();

		parameters.add(pcscPowerFunction.getAsByteArray());
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));

		PcscCallResult result = doPcsc(
				new PcscCallData(IfdInterface.PCSC_FUNCTION_POWER_ICC, lun, parameters));

		return PcscConstants.IFD_SUCCESS.equals(result.getResponseCode());
	}

	public byte[] pcscTransmit(byte[] inputApdu) {
		UnsignedInteger function = new UnsignedInteger(IfdInterface.VALUE_PCSC_FUNCTION_TRANSMIT_TO_ICC);

		List<byte[]> parameters = new LinkedList<>();

		parameters.add(inputApdu.clone());
		parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));

		PcscCallResult result = doPcsc(new PcscCallData(function, lun, parameters));

		if (PcscConstants.IFD_SUCCESS.equals(result.getResponseCode())) {
			return result.getData().get(0);
		}
		throw new IllegalStateException(
				"Call for transmit was not successful: " + result.getResponseCode().getAsHexString());
	}

	@Override
	public boolean isIccAvailable() {
		PcscCallResult result = doPcsc(new PcscCallData(IfdInterface.PCSC_FUNCTION_IS_ICC_PRESENT, new UnsignedInteger(0), Collections.emptyList()));
		return result.getResponseCode().equals(PcscConstants.IFD_ICC_PRESENT);
	}
	
	private String getStatusMessage(String slotName) {
		if (currentProtocol != null) {
			return currentProtocol.getStatusMessage(slotName, this).toString();
		} else {
			return null;
		}
	}

	@Override
	public String getStatusMessage() {
		return getStatusMessage(null);
	}

	@Override
	public String getContextHandle() {
		return contextHandle;
	}

	@Override
	public String getDeviceName() {
		return remoteIfdConfig.getName();
	}

}
