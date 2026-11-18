package de.persosim.websocket;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.json.JSONArray;
import org.json.JSONObject;

import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.simulator.platform.Iso7816Lib;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class IfdProtocolWebSocketV0 implements IfdProtocolWebSocket {
	
	public static String IDENTIFIER = "IFDInterface_WebSocket_v0";

	private static final String SLOT_HANDLE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvw0123456789";

	String slotName = "PersoSim Slot 1";
	String slotHandle = null;

	@Override
	public JSONObject message(JSONObject jsonMessage, IfdProtocolWebSocket.ContextProvider ctxProvider) {
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
		case IFD_ESTABLISH_CONTEXT:

			response.put(MSG, IFD_ESTABLISH_CONTEXT_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());

			response.put(IFD_NAME, ctxProvider.getDeviceName());

			setOkResult(response);
			return response;
		case IFD_CONNECT:
			response.put(MSG, IFD_CONNECT_RESPONSE);
			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			this.slotHandle  = createRandomSlotHandle(10);
			
			response.put(SLOT_HANDLE, this.slotHandle);

			if (ctxProvider.pcscPowerIcc(PcscConstants.IFD_POWER_UP)) {
				setOkResult(response);
			} else {
				setErrorResult(response, Tr03112codes.TERMINAL_RESULT_TERMINAL_NO_CARD);
			}
			break;
		case IFD_DISCONNECT:

			response.put(MSG, IFD_DISCONNECT_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_HANDLE, this.slotHandle);

			ctxProvider.pcscPowerIcc(PcscConstants.IFD_POWER_DOWN);
			setOkResult(response);

			this.slotHandle = null;
			break;
		case IFD_TRANSMIT:

			response.put(MSG, IFD_TRANSMIT_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_HANDLE, this.slotHandle);

			JSONArray commandApdus = jsonMessage.getJSONArray(COMMAND_APDUS);

			List<String> responseApdus = handleApdus(commandApdus, ctxProvider);
			
			response.put("ResponseAPDUs", new JSONArray(responseApdus));

			setOkResult(response);
			
			break;
		case IFD_GET_STATUS:
			String incomingSlotName = jsonMessage.getString(SLOT_NAME);
			return getStatusMessage(incomingSlotName, ctxProvider);
		case IFD_ESTABLISH_PACE_CHANNEL:
			response.put(MSG, IFD_ESTABLISH_PACE_CHANNEL_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_HANDLE, this.slotHandle);

			System.out.println("EstablishPace InputMessage: "+jsonMessage.getString("InputData"));
			byte[] pcscPerformEstablishPaceChannel = ctxProvider.pcscPerformEstablishPaceChannel(HexString.toByteArray(jsonMessage.getString("InputData")));
			
			if (pcscPerformEstablishPaceChannel == null) {
				setErrorResult(response, Tr03112codes.TERMINAL_RESULT_TERMINAL_ACCESS_ERROR);
			} else {
				response.put(OUTPUT_DATA, HexString.encode(
						pcscPerformEstablishPaceChannel));
				setOkResult(response);
			}

			break;
		case IFD_MODIFY_PIN:
			response.put(MSG, IFD_MODIFY_PIN_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_HANDLE, this.slotHandle);

			response.put("OutputData", HexString.encode(ctxProvider.pcscPerformModifyPin(HexString.toByteArray(jsonMessage.getString("InputData")))));

			setOkResult(response);

			break;
		case IFD_ERROR:
			return null;
		default:
			response.put(MSG, IFD_ERROR);
			response.put(RESULT_MAJOR, Tr03112codes.RESULT_MAJOR_ERROR);
			response.put(RESULT_MINOR, Tr03112codes.TERMINAL_RESULT_TERMINAL_UNKNOWN_ACTION);
		}

		BasicLogger.log(getClass(), "Send JSON message: " + System.lineSeparator() + response.toString(),
				LogLevel.TRACE);
		return response;
	}

	private List<String> handleApdus(JSONArray commandApdus, ContextProvider ctxProvider) {
		List<String> responseApdus = new LinkedList<>();
		for (int i = 0; i < commandApdus.length(); i++) {
			JSONObject currentApdu = commandApdus.getJSONObject(i);
			byte[] inputApdu = HexString.toByteArray(currentApdu.getString(INPUT_APDU));

			Number[] acceptableStatusCodes = null;
			if (!currentApdu.isNull(ACCEPTABLE_STATUS_CODES)) {
				JSONArray statusCodes = currentApdu.getJSONArray(ACCEPTABLE_STATUS_CODES);
				acceptableStatusCodes = getStatusCodesFromJson(statusCodes);
			}

			byte[] responseApdu = ctxProvider.pcscTransmit(inputApdu);

			short actualStatusCode = Iso7816Lib.getStatusWord(responseApdu);

			if (!checkStatusCodes(acceptableStatusCodes, actualStatusCode)) {
				break;
			}
			responseApdus.add(HexString.encode(responseApdu));
		}
		return responseApdus;
	}

	private boolean checkStatusCodes(Number[] acceptableStatusCodes, short actualStatusCode) {
		boolean statusCodeAcceptable = true;
		if (acceptableStatusCodes != null) {
			statusCodeAcceptable  = false;
			for (Number currentAcceptable : acceptableStatusCodes) {
				boolean isByteAndAcceptable = currentAcceptable instanceof Byte
						&& currentAcceptable.equals(Utils.getFirstByteOfShort(actualStatusCode));
				boolean isShortAndAcceptable = currentAcceptable instanceof Short
						&& currentAcceptable.equals(actualStatusCode);
				if (isByteAndAcceptable || isShortAndAcceptable) {
					statusCodeAcceptable = true;
					break;
				}
			}
		}
		return statusCodeAcceptable;
	}

	void setErrorResult(JSONObject response, String resultMinor) {
		response.put(RESULT_MAJOR, Tr03112codes.RESULT_MAJOR_ERROR);
		response.put(RESULT_MINOR, resultMinor);
	}

	void setOkResult(JSONObject response) {
		response.put(RESULT_MAJOR, Tr03112codes.RESULT_MAJOR_OK);
		response.put(RESULT_MINOR, JSONObject.NULL);
	}

	private JSONObject convertPscsCapabilitiesToJson(byte[] capabilities) {
		// We are expecting one length byte and one byte for the bit field
		if (capabilities.length != 2 || capabilities [0] != 1) {
			throw new IllegalArgumentException("PACE capabilities in unexpected format");
		}
		
		JSONObject jsonCaps = new JSONObject();
		jsonCaps.put("PACE", ((byte)(PersoSimPcscProcessor.BITMAP_IFD_GENERIC_PACE_SUPPORT & capabilities[1])) == PersoSimPcscProcessor.BITMAP_IFD_GENERIC_PACE_SUPPORT);
		jsonCaps.put("eID", ((byte)(PersoSimPcscProcessor.BITMAP_EID_APPLICATION_SUPPORT & capabilities[1])) == PersoSimPcscProcessor.BITMAP_EID_APPLICATION_SUPPORT);
		jsonCaps.put("eSign", ((byte)(PersoSimPcscProcessor.BITMAP_QUALIFIED_SIGNATURE_FUNCTION & capabilities[1])) == PersoSimPcscProcessor.BITMAP_QUALIFIED_SIGNATURE_FUNCTION);
		jsonCaps.put("Destroy", ((byte)(PersoSimPcscProcessor.BITMAP_IFD_DESTROY_CHANNEL_SUPPORT & capabilities[1])) == PersoSimPcscProcessor.BITMAP_IFD_DESTROY_CHANNEL_SUPPORT);
		return jsonCaps;
	}

	private Number[] getStatusCodesFromJson(JSONArray jsonArray) {
		Number[] result = new Number[jsonArray.length()];
		for (int i = 0; i < jsonArray.length(); i++) {
			int statusCodeLength = jsonArray.getString(i).length();
			if (statusCodeLength == 1) {
				result[i] = Byte.parseByte(jsonArray.getString(i), 16);
			} else if (statusCodeLength == 2) {
				result[i] = Short.parseShort(jsonArray.getString(i), 16);
			} else {
				throw new IllegalArgumentException("Status code can not have length " + statusCodeLength);
			}
		}
		return result;
	}

	private String createRandomSlotHandle(int length) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < length; i++) {
			builder.append(SLOT_HANDLE_CHARACTERS.charAt(ThreadLocalRandom.current().nextInt(SLOT_HANDLE_CHARACTERS.length())));
		}
		return builder.toString();
	}
	
	public JSONObject getStatusMessage(String slotName, ContextProvider ctxProvider) {
		if (slotName == null || slotName.isEmpty() || (slotName.equals(this.slotName))) {
			JSONObject response = new JSONObject();
			response.put(MSG, "IFDStatus");
			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_NAME, this.slotName);

			response.put(PIN_CAPABILITIES, convertPscsCapabilitiesToJson(ctxProvider.pcscPerformGetReaderPaceCapabilities()));
			response.put(MAX_APDU_LENGTH, Short.MAX_VALUE);
			response.put(CONNECTED_READER, true);
			response.put(CARD_AVAILABLE, ctxProvider.isIccAvailable());
			response.put(EFATR, JSONObject.NULL);
			response.put(EFDIR, JSONObject.NULL);
			return response;
		} else {
			return null;
		}
	}

}
