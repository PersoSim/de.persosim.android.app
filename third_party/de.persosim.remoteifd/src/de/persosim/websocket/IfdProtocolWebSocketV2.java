package de.persosim.websocket;

import java.util.LinkedList;
import java.util.List;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;
import org.json.JSONObject;

import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.features.PersoSimPcscProcessor;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class IfdProtocolWebSocketV2 extends IfdProtocolWebSocketV0 {

	public static String IDENTIFIER = "IFDInterface_WebSocket_v2";

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
		case IFD_TRANSMIT:

			response.put(MSG, IFD_TRANSMIT_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_HANDLE, this.slotHandle);

			String commandApdu = jsonMessage.getString(INPUT_APDU);

			String responseApdus = handleApdu(commandApdu, ctxProvider);
			
			response.put(RESPONSE_APDU, responseApdus);

			setOkResult(response);
			
			break;
		case IFD_GET_STATUS:
			String incomingSlotName = jsonMessage.getString(SLOT_NAME);
			return getStatusMessage(incomingSlotName, ctxProvider);
		case IFD_ESTABLISH_PACE_CHANNEL:
			response.put(MSG, IFD_ESTABLISH_PACE_CHANNEL_RESPONSE);

			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_HANDLE, this.slotHandle);

			List<byte[]> parameters = new LinkedList<>();
			UnsignedInteger controlCode = ctxProvider.pcscPerformGetFeatures().get(PersoSimPcscProcessor.FEATURE_CONTROL_CODE);
			parameters.add(controlCode.getAsByteArray());

			if (jsonMessage.has("InputData")) {
				byte[] function = new byte[] {PersoSimPcscProcessor.FUNCTION_ESTABLISH_PACE_CHANNEL};
				byte[] inputData = HexString.toByteArray(jsonMessage.getString("InputData"));
				byte[] lenInputData = Utils.createLengthValue(inputData, 2);
				parameters.add(Utils.concatByteArrays(function, lenInputData, inputData));
			}

			parameters.add(Utils.toShortestUnsignedByteArray(Integer.MAX_VALUE));
			
			PcscCallResult result = ctxProvider.doPcsc(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL, parameters);
			
			
			
			if (result != null) {
				byte[] outputData = result.getData().get(0);
				
				response.put(RESULT_CODE, HexString.encode(outputData).substring(0,8));
				
				response.put(OUTPUT_DATA, HexString.encode(outputData).substring(12)); //remove leading resultcode and length field
				setOkResult(response);
				
			} else {
				setErrorResult(response, Tr03112codes.TERMINAL_RESULT_TERMINAL_ACCESS_ERROR);
			}

			break;
		default:
			return super.message(jsonMessage, ctxProvider);
		}

		return response;
	}

	private String handleApdu(String commandApdu, ContextProvider ctxProvider) {
		byte[] inputApdu = HexString.toByteArray(commandApdu);

		byte[] responseApdu = ctxProvider.pcscTransmit(inputApdu);

		return HexString.encode(responseApdu);
	}

	private boolean isPacePinPadAvailable(byte[] capabilities) {
		// We are expecting one length byte and one byte for the bit field
		if (capabilities.length != 2 || capabilities [0] != 1) {
			return false;
		}
		
		return ((byte)(PersoSimPcscProcessor.BITMAP_IFD_GENERIC_PACE_SUPPORT & capabilities[1])) == PersoSimPcscProcessor.BITMAP_IFD_GENERIC_PACE_SUPPORT;
		
	}

	@Override
	public JSONObject getStatusMessage(String slotName, ContextProvider ctxProvider) {
		if (slotName == null || slotName.isEmpty() || (slotName.equals(this.slotName))) {
			JSONObject response = new JSONObject();
			response.put(MSG, "IFDStatus");
			response.put(CONTEXT_HANDLE, ctxProvider.getContextHandle());
			response.put(SLOT_NAME, this.slotName);

			response.put(PIN_PAD, isPacePinPadAvailable(ctxProvider.pcscPerformGetReaderPaceCapabilities()));
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
