package de.persosim.driver.connector.features;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import de.persosim.driver.connector.CommUtils;
import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.SimulatorManager;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.exceptions.PaceExecutionException;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.UiEnabled;
import de.persosim.simulator.platform.Iso7816Lib;
import de.persosim.simulator.protocols.Tr03110;
import de.persosim.simulator.tlv.PrimitiveTlvDataObject;
import de.persosim.simulator.tlv.TlvConstants;
import de.persosim.simulator.tlv.TlvDataObject;
import de.persosim.simulator.tlv.TlvDataObjectContainer;
import de.persosim.simulator.tlv.TlvTag;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

/**
 * This class gets PCSC style messages and processes them.
 * 
 * @author mboonk
 *
 */
public class PersoSimPcscProcessor extends AbstractPcscFeature implements
		PcscConstants, UiEnabled {

	/**
	 * This enum represents the different states of the PCSC pseudo PACE.
	 * 
	 * @author mboonk
	 *
	 */
	public enum PaceState {
		NO_SM, SM_ESTABLISHED;
	}

	private List<VirtualReaderUi> interfaces;

	public static final byte FUNCTION_GET_READER_PACE_CAPABILITIES = 1;
	public static final byte FUNCTION_ESTABLISH_PACE_CHANNEL = 2;
	public static final byte FUNCTION_DESTROY_PACE_CHANNEL = 3;

	public static final int VALUE_RESULT_NO_ERROR = 0x00000000;
	// Errors in input data
	public static final int VALUE_RESULT_INCONSISTENT_LENGTHS = 0xD0000001;
	public static final int VALUE_RESULT_UNEXPECTED_DATA = 0xD0000002;
	public static final int VALUE_RESULT_UNEXPECTED_DATA_COMBINATION = 0xD0000003;
	// Errors during protocol execution
	public static final int VALUE_RESULT_TLV_SYNTAX_ERROR = 0xE0000001;
	public static final int VALUE_RESULT_UNEXPECTED_OR_MISSING_TLV_OBJECT = 0xE0000002;
	public static final int VALUE_RESULT_UNKNOWN_PIN_ID = 0xE0000003;
	public static final int VALUE_RESULT_WRONG_AUTHENTICATION_TOKEN = 0xE0000006;
	// Response APDU of the card reports error (status code SW1SW2)
	public static final short RESULT_PREFIX_SELECT_EF_CA = (short) 0xF000;
	public static final short RESULT_PREFIX_READ_BINARY_EF_CA = (short) 0xF001;
	public static final short RESULT_PREFIX_MSE_SET_AT = (short) 0xF002;
	public static final short RESULT_PREFIX_GAT_1 = (short) 0xF003;
	public static final short RESULT_PREFIX_GAT_2 = (short) 0xF004;
	public static final short RESULT_PREFIX_GAT_3 = (short) 0xF005;
	public static final short RESULT_PREFIX_GAT_4 = (short) 0xF006;
	// Others
	public static final int VALUE_RESULT_COMMUNICATION_ABORT = 0xF0100001;
	public static final int VALUE_RESULT_NO_CARD = 0xF0100002;
	public static final int VALUE_RESULT_ABORT = 0xF0200001;
	public static final int VALUE_RESULT_TIMEOUT = 0xF0200002;

	public static final UnsignedInteger RESULT_NO_ERROR = new UnsignedInteger(
			VALUE_RESULT_NO_ERROR);
	public static final UnsignedInteger RESULT_INCONSISTENT_LENGTHS = new UnsignedInteger(
			VALUE_RESULT_INCONSISTENT_LENGTHS);
	public static final UnsignedInteger RESULT_UNEXPECTED_DATA = new UnsignedInteger(
			VALUE_RESULT_UNEXPECTED_DATA);
	public static final UnsignedInteger RESULT_UNEXPECTED_DATA_COMBINATION = new UnsignedInteger(
			VALUE_RESULT_UNEXPECTED_DATA_COMBINATION);
	public static final UnsignedInteger RESULT_TLV_SYNTAX_ERROR = new UnsignedInteger(
			VALUE_RESULT_TLV_SYNTAX_ERROR);
	public static final UnsignedInteger RESULT_UNEXPECTED_OR_MISSING_TLV_OBJECT = new UnsignedInteger(
			VALUE_RESULT_UNEXPECTED_OR_MISSING_TLV_OBJECT);
	public static final UnsignedInteger RESULT_COMMUNICATION_ABORT = new UnsignedInteger(
			VALUE_RESULT_COMMUNICATION_ABORT);
	public static final UnsignedInteger RESULT_NO_CARD = new UnsignedInteger(
			VALUE_RESULT_NO_CARD);
	public static final UnsignedInteger RESULT_ABORT = new UnsignedInteger(
			VALUE_RESULT_ABORT);
	public static final UnsignedInteger RESULT_TIMEOUT = new UnsignedInteger(
			VALUE_RESULT_TIMEOUT);

	public static final byte BITMAP_IFD_GENERIC_PACE_SUPPORT = 0x40;
	public static final byte BITMAP_IFD_DESTROY_CHANNEL_SUPPORT = (byte) 0x80;
	public static final byte BITMAP_EID_APPLICATION_SUPPORT = (byte) 0x20;
	public static final byte BITMAP_QUALIFIED_SIGNATURE_FUNCTION = (byte) 0x10;

	private static final int LENGTH_ESTABLISH_PACE_CHAT_LENGTH = 1;
	private static final int LENGTH_ESTABLISH_PACE_PIN_LENGTH = 1;

	public static final byte FEATURE_CONTROL_CODE = 0x20;

	public static final int OFFSET_FUNCTION = 0;
	public static final int OFFSET_DATA_LENGTH = 1;
	public static final int OFFSET_INPUT_DATA = 3;
	public static final int OFFSET_ESTABLISH_PACE_PIN_ID = 3;
	public static final int OFFSET_COMMAND_DATA = 1;
	public static final int OFFSET_LC = 0;

	private PaceState currentState = PaceState.NO_SM;

	/**
	 * This creates a PACE-feature using the given control code.
	 * 
	 * @param controlCode
	 *            the control code to use
	 */
	public PersoSimPcscProcessor(UnsignedInteger controlCode) {
		super(controlCode, FEATURE_CONTROL_CODE);
	}

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		switch (data.getFunction().getAsInt()) {
		case IfdInterface.VALUE_PCSC_FUNCTION_TRANSMIT_TO_ICC:
			return transmitToIcc(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_DEVICE_CONTROL:
			return deviceControl(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_POWER_ICC:
			return powerIcc(data);
		default:
			return null;
		}
	}

	/**
	 * Override the default behavior of by processing PACE related messages and
	 * ignoring all others.
	 * 
	 * @param data
	 * @return the PACE result data
	 */
	private PcscCallResult deviceControl(PcscCallData data) {
		UnsignedInteger controlCode = new UnsignedInteger(data.getParameters()
				.get(0));
		UnsignedInteger expectedLength = null;
		if (2 < data.getParameters().size()) {
			expectedLength = new UnsignedInteger(data.getParameters().get(2));
		}

		if (expectedLength == null) {
			return new SimplePcscCallResult(
					PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}

		if (controlCode.equals(getControlCode())) {
			switch (data.getParameters().get(1)[OFFSET_FUNCTION]) {
			case FUNCTION_GET_READER_PACE_CAPABILITIES:
				return getReaderPaceCapabilities();
			case FUNCTION_ESTABLISH_PACE_CHANNEL:
				return establishPaceChannel(data.getParameters().get(1));
			case FUNCTION_DESTROY_PACE_CHANNEL:
				return destroyPaceChannel(data.getParameters().get(1));
			}
		}
		return null;
	}

	/**
	 * Capture PSCS power events and set the pseudo secure messaging state
	 * accordingly.
	 * 
	 * @param data
	 * @return null, to allow further processing
	 */
	private PcscCallResult powerIcc(PcscCallData data) {
		UnsignedInteger action = new UnsignedInteger(data.getParameters()
				.get(0));

		if (IFD_POWER_DOWN.equals(action) || IFD_POWER_UP.equals(action)
				|| IFD_RESET.equals(action)) {
			currentState = PaceState.NO_SM;
		}
		return null;
	}

	/**
	 * An APDU will be modified by setting the logical channel bits to 1 but not
	 * send. This method can also be used to execute PACE commands using PPDUs
	 * with an FFC20120 prefix.
	 * 
	 * @param data
	 * @return
	 */
	private PcscCallResult transmitToIcc(PcscCallData data) {
		byte[] commandPpdu = data.getParameters().get(0);

		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null) {
			return new SimplePcscCallResult(
					PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}

		byte[] expectedHeader = new byte[] { (byte) 0xff, (byte) 0xc2, 0x01,
				FEATURE_CONTROL_CODE };

		if (!Utils.arrayHasPrefix(commandPpdu, expectedHeader)) {
			if (currentState.equals(PaceState.SM_ESTABLISHED)) {
				// destroy the channel when a secure messaging apdu arrives
				if ((commandPpdu[Iso7816Lib.OFFSET_CLA] & 0x0c) == 0x0c) {
					currentState = PaceState.NO_SM;
					return null;
				}
				// set logical channel to 3
				commandPpdu[Iso7816Lib.OFFSET_CLA] |= 0b011;
			}

			return null;
		}

		PcscCallResult result = null;
		switch (commandPpdu[OFFSET_FUNCTION]) {
		case FUNCTION_GET_READER_PACE_CAPABILITIES:
			result = getReaderPaceCapabilities();
		case FUNCTION_ESTABLISH_PACE_CHANNEL:
			result = establishPaceChannel(getInputDataFromPpdu(commandPpdu));
		case FUNCTION_DESTROY_PACE_CHANNEL:
			result = destroyPaceChannel(getInputDataFromPpdu(commandPpdu));
		}

		if (result != null
				&& result.getEncoded().length() - 9 > expectedLength
						.getAsSignedLong()) {
			return new SimplePcscCallResult(
					PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}

		return null;
	}

	private PcscCallResult destroyPaceChannel(byte[] inputDataFromPpdu) {
		// not supported
		return null;
	}
	
	/**
	 * Establish a pseudo PACE channel between the connector and the simulator
	 * @param inputDataFromPpdu
	 * @return
	 */
	private PcscCallResult establishPaceChannel(byte[] inputDataFromPpdu) {
		System.out.println("PCSCPACE");
		System.out.println(HexString.dump(inputDataFromPpdu));
		
		int offset = OFFSET_ESTABLISH_PACE_PIN_ID;
		byte pinId = inputDataFromPpdu[offset];
		byte[] chat = Utils.getValue(inputDataFromPpdu, ++offset,
				LENGTH_ESTABLISH_PACE_CHAT_LENGTH);
		offset += chat.length + LENGTH_ESTABLISH_PACE_CHAT_LENGTH;
		byte[] pin = Utils.getValue(inputDataFromPpdu, offset,
				LENGTH_ESTABLISH_PACE_PIN_LENGTH);
		offset += pin.length + LENGTH_ESTABLISH_PACE_PIN_LENGTH;

		displayChat(chat);
		try {
			if (pin.length == 0) {
				pin = getPinFromInterfaces(pinId);

			}
		} catch (IOException e) {
			// #833 Use proper logging mechanism
			return new SimplePcscCallResult(IFD_ERROR_NOT_SUPPORTED);
		} finally {
			displayChat(null);
		}

		if (pin == null) {
			return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_ABORT,
					new byte[0]);
		}

		TlvDataObjectContainer data = buildPseudoApduData(pinId, chat, pin);

		try {

			byte[] efCardAccess = readCardAccess();

			byte[] pseudoApduHeader = new byte[] { (byte) 0xff, (byte) 0x86, 0,
					0, (byte) (0xFF & data.toByteArray().length) };
			byte[] responseApdu = SimulatorManager.getSim()
					.processCommand(
							Utils.concatByteArrays(pseudoApduHeader,
									data.toByteArray()));

			byte[] pcscResponse = buildPcscResponseData(efCardAccess,
					responseApdu);
			currentState = PaceState.SM_ESTABLISHED;
			return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_NO_ERROR,
					pcscResponse);

		} catch (IOException e) {
			// #833 Use proper logging mechanism
			return buildResponse(PcscConstants.IFD_SUCCESS,
					RESULT_COMMUNICATION_ABORT, new byte[0]);
		} catch (PaceExecutionException e) {
			return buildResponse(PcscConstants.IFD_SUCCESS, e.getError(),
					e.getResponseBuffer());
		}

	}

	private void displayChat(byte[] chat) {
		for (VirtualReaderUi currentUi : interfaces) {
			currentUi.displayChat(chat);
		}	
	}

	/**
	 * Query all interfaces for a password and return it.
	 * 
	 * @param pin
	 * @return the password
	 * @throws IOException
	 */
	private byte[] getPinFromInterfaces(byte pinType) throws IOException {
		byte [] pin = null;

		if (interfaces != null) {
			for (VirtualReaderUi current : interfaces) {
				String toDisplay = "Enter ";
				switch (pinType){
				case Tr03110.ID_CAN:
					toDisplay += "CAN";
					break;
				case Tr03110.ID_PIN:
					toDisplay += "PIN";
					break;
				case Tr03110.ID_PUK:
					toDisplay += "PUK";
					break;
				case Tr03110.ID_MRZ:
					toDisplay += "MRZ";
					break;
				}
				current.display(toDisplay);
				pin = current.getPin();
				if (pin != null) {
					break;
				}
			}
			return pin;
		}
		return null;
	}

	/**
	 * Construct the PCSC response for an establish PACE command from the
	 * previously read EF.CA and the response data of the pseudo PACE apdu.
	 * 
	 * @param efCardAccess
	 * @param responseApdu
	 * @return the response data to be wrapped into a PCSC-PACE answer
	 */
	private byte[] buildPcscResponseData(byte[] efCardAccess, byte[] responseApdu) throws PaceExecutionException {
		TlvDataObjectContainer responseData = new TlvDataObjectContainer(
				Arrays.copyOf(responseApdu, responseApdu.length - 2));

		byte[] paceStatusWord = Arrays.copyOfRange(responseApdu,
				responseApdu.length - 2, responseApdu.length);
		
		TlvDataObject setAtStatusWord = responseData
				.getTlvDataObject(TlvConstants.TAG_80);

		if (setAtStatusWord == null){
			throw new PaceExecutionException(RESULT_COMMUNICATION_ABORT, new byte [0]);
		}
		
		short setAtStatusWordValue = Utils.getShortFromUnsignedByteArray(setAtStatusWord.getValueField()); 
		if (Iso7816Lib.isReportingError(setAtStatusWordValue)){
			throw new PaceExecutionException(new UnsignedInteger(
					Utils.concatByteArrays(
							Utils.toUnsignedByteArray(RESULT_PREFIX_MSE_SET_AT),
							Utils.toUnsignedByteArray(setAtStatusWordValue))), new byte [0]);
		}
		
		TlvDataObject currentCar = responseData
				.getTlvDataObject(TlvConstants.TAG_87);
		TlvDataObject previousCar = responseData
				.getTlvDataObject(TlvConstants.TAG_88);
		TlvDataObject idIcc = responseData
				.getTlvDataObject(TlvConstants.TAG_86);

		if (currentCar == null) {
			currentCar = new PrimitiveTlvDataObject(new TlvTag((byte) 0));
		}
		if (previousCar == null) {
			previousCar = new PrimitiveTlvDataObject(new TlvTag((byte) 0));
		}
		if (idIcc == null) {
			idIcc = new PrimitiveTlvDataObject(new TlvTag((byte) 0));
		}

		byte[] pcscResponse = Utils
				.concatByteArrays(
						setAtStatusWord.getValueField(),
						CommUtils
								.toUnsignedShortFlippedBytes((short) efCardAccess.length),
						efCardAccess,
						new byte[] { (byte) (0xFF & currentCar
								.getLengthValue()) }, currentCar
								.getValueField(),
						new byte[] { (byte) (0xFF & previousCar
								.getLengthValue()) }, previousCar
								.getValueField(), CommUtils
								.toUnsignedShortFlippedBytes((short) idIcc
										.getLengthValue()), idIcc
								.getValueField());

		short paceStatusWordValue = Utils.getShortFromUnsignedByteArray(paceStatusWord);
		
		if (!Iso7816Lib.isReportingNormalProcessing(paceStatusWordValue)) {
			throw new PaceExecutionException(new UnsignedInteger(
					Utils.concatByteArrays(
							Utils.toUnsignedByteArray(RESULT_PREFIX_GAT_4),
							Utils.toUnsignedByteArray(paceStatusWordValue))),
					pcscResponse);
		}
				
		return pcscResponse;
	}

	/**
	 * Reads the EF.CA from the simulator.
	 * @return the file contents
	 * @throws IOException
	 */
	private byte[] readCardAccess() throws PaceExecutionException, IOException {
		byte[] select = HexString.toByteArray("00 A4 02 0C 02 01 1C");
		byte[] readBinary = HexString.toByteArray("00 B0 00 00 00");

		byte[] response = SimulatorManager.getSim().processCommand(select);
		
		short statusWord = Utils
				.getShortFromUnsignedByteArray(Arrays.copyOfRange(response,
						response.length - 2, response.length - 1));

		if (Iso7816Lib.isReportingError(statusWord)) {
			// #833 Use proper logging mechanism			
			throw new PaceExecutionException(new UnsignedInteger(Utils.concatByteArrays(
					Utils.toUnsignedByteArray(RESULT_PREFIX_SELECT_EF_CA),
					Utils.toUnsignedByteArray(statusWord))), new byte [0]);
		}
		
		response = SimulatorManager.getSim().processCommand(readBinary);

		statusWord = Utils
				.getShortFromUnsignedByteArray(Arrays.copyOfRange(response,
						response.length - 2, response.length - 1));

		if (Iso7816Lib.isReportingError(statusWord)) {
			// #833 Use proper logging mechanism			
			throw new PaceExecutionException(new UnsignedInteger(Utils.concatByteArrays(
					Utils.toUnsignedByteArray(RESULT_PREFIX_READ_BINARY_EF_CA),
					Utils.toUnsignedByteArray(statusWord))), new byte [0]);
		}

		return Arrays.copyOf(response, response.length - 2);
	}

	/**
	 * Builds the command APDU that is used to trigger the PACE initialization
	 * in the simulator.
	 * 
	 * @param pinId
	 * @param chat
	 * @param pin
	 * @return the command data
	 */
	private TlvDataObjectContainer buildPseudoApduData(byte pinId, byte[] chat,
			byte[] pin) {
		TlvDataObjectContainer data = new TlvDataObjectContainer();
		// add password id
		data.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_83,
				new byte[] { pinId }));
		// add password data
		data.addTlvDataObject(new PrimitiveTlvDataObject(TlvConstants.TAG_92,
				pin));
		// add chat
		if (chat.length > 0) {
			data.addTlvDataObject(new TlvDataObjectContainer(chat).getTlvDataObject(TlvConstants.TAG_7F4C));
		}
		return data;
	}

	/**
	 * @return this features PACE-capabilities
	 */
	private PcscCallResult getReaderPaceCapabilities() {
		// The PACE capabilities are stored in a byte array preceded by a
		// singular length byte and wrapped into the standard pace feature response
		byte[] bitMap = new byte[] {1, BITMAP_IFD_GENERIC_PACE_SUPPORT
				| BITMAP_EID_APPLICATION_SUPPORT };
		return buildResponse(PcscConstants.IFD_SUCCESS, RESULT_NO_ERROR, bitMap);
	}

	/**
	 * Wraps arbitrary data into the PCSC-PACE-features response format.
	 * 
	 * @param pcscResponseCode
	 * @param result
	 * @param resultData
	 * @return
	 */
	private PcscCallResult buildResponse(UnsignedInteger pcscResponseCode,
			UnsignedInteger paceResponseCode, byte[] resultData) {
		byte[] response = Utils.concatByteArrays(CommUtils.changeByteOrder(paceResponseCode.getAsByteArray()), CommUtils
				.toUnsignedShortFlippedBytes((short) resultData.length),
				resultData);

		System.out.println("PCSCPACE Result");
		System.out.println(HexString.dump(response));
		
		return new SimplePcscCallResult(pcscResponseCode, response);
	}

	/**
	 * Extract the input data from a PCSC PPDU.
	 * 
	 * @param ppdu
	 * @return the data field
	 */
	private byte[] getInputDataFromPpdu(byte[] ppdu) {
		return Arrays.copyOfRange(ppdu, OFFSET_INPUT_DATA, Utils
				.getShortFromUnsignedByteArray(Arrays.copyOfRange(ppdu,
						OFFSET_DATA_LENGTH, OFFSET_INPUT_DATA
								+ OFFSET_COMMAND_DATA)));
	}

	@Override
	public void setUserInterfaces(List<VirtualReaderUi> interfaces) {
		this.interfaces = interfaces;
	}
}
