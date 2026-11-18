package de.persosim.websocket;

import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.pcsc.PcscCallResult;

/**
 * Implementations of different variations of IfdProtocolWebSocket.
 * 
 * {@link DefaultMessageHandler} handles protocol selection and forwards all
 * other messages to the selected protocol implementation.
 * 
 * @author Alexander May
 *
 */
public interface IfdProtocolWebSocket {
	
	public static final String IFD_GET_STATUS = "IFDGetStatus";
	public static final String IFD_ESTABLISH_PACE_CHANNEL_RESPONSE = "IFDEstablishPACEChannelResponse";
	public static final String IFD_ESTABLISH_PACE_CHANNEL = "IFDEstablishPACEChannel";
	public static final String IFD_NAME = "IFDName";
	public static final String IFD_TRANSMIT = "IFDTransmit";
	public static final String IFD_TRANSMIT_RESPONSE = "IFDTransmitResponse";
	public static final String IFD_ESTABLISH_CONTEXT = "IFDEstablishContext";
	public static final String IFD_ESTABLISH_CONTEXT_RESPONSE = "IFDEstablishContextResponse";
	public static final String IFD_CONNECT = "IFDConnect";
	public static final String IFD_CONNECT_RESPONSE = "IFDConnectResponse";
	public static final String IFD_DISCONNECT = "IFDDisconnect";
	public static final String IFD_DISCONNECT_RESPONSE = "IFDDisconnectResponse";
	public static final String IFD_ERROR = "IFDERROR";
	public static final String IFD_MODIFY_PIN = "IFDModifyPIN";
	public static final String IFD_MODIFY_PIN_RESPONSE = "IFDModifyPINResponse";
	
	public static final String MSG = "msg";
	public static final String RESULT_MINOR = "ResultMinor";
	public static final String RESULT_MAJOR = "ResultMajor";
	public static final String RESULT_CODE = "ResultCode";
	public static final String PROTOCOL = "Protocol";
	public static final String SLOT_HANDLE = "SlotHandle";
	public static final String CONTEXT_HANDLE = "ContextHandle";
	public static final String SLOT_NAME = "SlotName";
	public static final String COMMAND_APDUS = "CommandAPDUs"; 
	public static final String INPUT_APDU = "InputAPDU"; 
	public static final String RESPONSE_APDU = "ResponseAPDU";
	public static final String ACCEPTABLE_STATUS_CODES = "AcceptableStatusCodes";
	public static final String EFDIR = "EFDIR";
	public static final String EFATR = "EFATR";
	public static final String CARD_AVAILABLE = "CardAvailable";
	public static final String CONNECTED_READER = "ConnectedReader";
	public static final String MAX_APDU_LENGTH = "MaxAPDULength";
	public static final String PIN_CAPABILITIES = "PINCapabilities";
	public static final String PIN_PAD = "PINPad";
	public static final String OUTPUT_DATA = "OutputData";

	public static final String UD_NAME = "UDName";

	public JSONObject message(JSONObject jsonMessage, ContextProvider ctxProvider);
	
	public Object getStatusMessage(String slotName, ContextProvider ctxProvider);
	
	public interface ContextProvider {

		String getContextHandle();

		String getDeviceName();

		boolean isIccAvailable();

		byte[] pcscPerformGetReaderPaceCapabilities();

		byte[] pcscTransmit(byte[] inputApdu);

		byte[] pcscPerformModifyPin(byte[] byteArray);

		byte[] pcscPerformEstablishPaceChannel(byte[] byteArray);

		boolean pcscPowerIcc(UnsignedInteger ifdPowerUp);

		PcscCallResult doPcsc(UnsignedInteger function, List<byte[]> parameters);

		Map<Byte, UnsignedInteger> pcscPerformGetFeatures();
		
	}

}
