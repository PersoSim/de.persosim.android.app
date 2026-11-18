package de.persosim.driver.connector.features;

import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER;
import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_ICC_NOT_PRESENT;
import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_ICC_PRESENT;
import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_POWER_DOWN;
import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_POWER_UP;
import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_RESET;
import static de.persosim.driver.connector.pcsc.PcscConstants.IFD_SUCCESS;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_IFD_ATR;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_IFD_SIMULTANEOUS_ACCESS;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_IFD_SLOTS_NUMBER;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_IFD_SLOT_THREAD_SAFE;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_VENDOR_NAME;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_VENDOR_SERIAL;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_VENDOR_TYPE;
import static de.persosim.driver.connector.pcsc.PcscConstants.TAG_VENDOR_VERSION;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.CommUtils;
import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.SimulatorManager;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.ConnectorEnabled;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.PcscFeature;
import de.persosim.driver.connector.pcsc.PcscListener;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.UiEnabled;
import de.persosim.driver.connector.service.IfdConnector;
import de.persosim.simulator.platform.Iso7816;
import de.persosim.simulator.utils.Utils;

/**
 * This implements basic PC/SC behaviour for the driver connector.
 * @author mboonk
 *
 */
public class DefaultListener implements PcscListener, ConnectorEnabled, UiEnabled {

	private static final byte FEATURE_GET_FEATURE_REQUEST = 0;
	private byte[] cachedAtr = null;
	private IfdConnector connector;
	private List<VirtualReaderUi> userInterfaces;
	
	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		switch (data.getFunction().getAsInt()) {
		case IfdInterface.VALUE_PCSC_FUNCTION_DEVICE_LIST_DEVICES:
			return deviceListDevices();
		case IfdInterface.VALUE_PCSC_FUNCTION_DEVICE_CONTROL:
			return deviceControl(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_GET_CAPABILITIES:
			return getCapabilities(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_SET_CAPABILITIES:
			return setCapabilities(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_POWER_ICC:
			return powerIcc(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_TRANSMIT_TO_ICC:
			return transmitToIcc(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_IS_ICC_PRESENT:
			return isIccPresent(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_EJECT_ICC:
			return ejectIcc(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_GET_IFDSP:
			return getIfdsp(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_IS_CONTEXT_SUPPORTED:
			return isContextSupported(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_IS_ICC_ABSENT:
			return isIccAbsent(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_LIST_CONTEXTS:
			return listContexts(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_LIST_INTERFACES:
			return listInterfaces(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_SET_PROTOCOL_PARAMETERS:
			return setProtocolParameters(data);
		case IfdInterface.VALUE_PCSC_FUNCTION_SWALLOW_ICC:
			return swallowIcc(data);
		}
		return null;
	}

	private PcscCallResult deviceListDevices() {
		// logical card slot
		byte[] result = Utils.concatByteArrays(CommUtils
				.getNullTerminatedAsciiString("PersoSim Virtual Reader Slot"),
				PcscConstants.DEVICE_TYPE_SLOT.getAsByteArray());

		for (VirtualReaderUi ui : connector.getUserInterfaces()) {
			result = Utils.concatByteArrays(result, ui.getDeviceDescriptors());
		}

		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, result);
	}

	private PcscCallResult deviceControl(PcscCallData data) {
		UnsignedInteger controlCode = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 2);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (controlCode.equals(PcscConstants.CONTROL_CODE_GET_FEATURE_REQUEST)) {
			List<byte[]> features = getFeatures();
			byte[] resultData = new byte[0];
			for (int i = 0; i < features.size(); i++) {
				resultData = Utils
						.concatByteArrays(resultData, features.get(i));
			}
			if (resultData.length > expectedLength.getAsSignedLong()){
				return new SimplePcscCallResult(IFD_ERROR_INSUFFICIENT_BUFFER);
			}
			return new SimplePcscCallResult(IFD_SUCCESS, resultData);
		}
		return null;
	}

	private PcscCallResult getCapabilities(PcscCallData data) {
		// try to find tag in own capabilities
		byte [] result = null;
		UnsignedInteger currentTag = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (TAG_VENDOR_NAME.equals(currentTag)) {
			result = "secunet Security Networks AG".getBytes(StandardCharsets.US_ASCII);
		} else if (TAG_VENDOR_TYPE.equals(
				currentTag)) {
			result = "Virtual Card Reader IFD"
							.getBytes(StandardCharsets.US_ASCII);
		} else if (TAG_VENDOR_VERSION.equals(
				currentTag)) {
			result = new byte[] { 0,
							0, 0, 0 }; // 0xMMmmbbbb MM=major mm=minor
										// bbbb=build
		} else if (TAG_VENDOR_SERIAL.equals(
				currentTag)) {
			result = "Serial000000001".getBytes(StandardCharsets.US_ASCII);
		} else if (TAG_IFD_ATR.equals(
				currentTag)) {
			if (cachedAtr != null) {
				result = cachedAtr;
			}
		} else if (TAG_IFD_SIMULTANEOUS_ACCESS.equals(
				currentTag)) {
			result = 					new byte[] { 1 };
		} else if (TAG_IFD_SLOTS_NUMBER.equals( currentTag)) {
			result = 
					new byte[] { 1 };
		} else if (TAG_IFD_SLOT_THREAD_SAFE.equals(
						currentTag)) {
			result = 
					new byte[] { 0 };
		}

		if (result != null) {
			if (result.length > expectedLength.getAsSignedLong()){
				return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
			}
			return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, result);
		} 
		return new SimplePcscCallResult(PcscConstants.IFD_ERROR_TAG);
	}

	private PcscCallResult setCapabilities(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult setProtocolParameters(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult powerIcc(PcscCallData data) {
		UnsignedInteger action = new UnsignedInteger(data.getParameters().get(0));
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		if (SimulatorManager.getSim() == null){
			BasicLogger.log(getClass(), "The simulator service is not available", LogLevel.WARN);
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
		}
				
		if (IFD_POWER_DOWN.equals(
				action)) {
			if (SimulatorManager.getSim().isRunning()) {
				byte[] result;
				
				result = SimulatorManager.getSim().cardPowerDown();
				
				if (Arrays.equals(result,
						Utils.toUnsignedByteArray(Iso7816.SW_9000_NO_ERROR))) {
					return new SimplePcscCallResult(IFD_SUCCESS);
				}
				return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
			}
			return new SimplePcscCallResult(IFD_SUCCESS); //already powered down
		} else {
			if (IFD_POWER_UP.equals(action)) {
				if (SimulatorManager.getSim().isRunning())
					cachedAtr = SimulatorManager.getSim().cardPowerUp();
				else 
					BasicLogger.log(getClass(), "The simulator is not running, card was not powered up", LogLevel.WARN);
			} else if (IFD_RESET.equals(
					action)) {
				
				cachedAtr = SimulatorManager.getSim().cardReset();
			}
			
			if (cachedAtr == null) {
				return new SimplePcscCallResult(PcscConstants.IFD_ERROR_POWER_ACTION);
			}
			
			if (cachedAtr != null && cachedAtr.length <= expectedLength.getAsSignedLong()){
				return new SimplePcscCallResult(IFD_SUCCESS, cachedAtr);	
			}
			return new SimplePcscCallResult(IFD_ERROR_INSUFFICIENT_BUFFER);
		}
	}

	private PcscCallResult swallowIcc(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult ejectIcc(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult transmitToIcc(PcscCallData data) {
		byte[] commandApdu = data.getParameters().get(0);
		UnsignedInteger expectedLength = CommUtils.getExpectedLength(data, 1);

		if (expectedLength == null){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		byte[] expectedHeaderAndLc = new byte[] { (byte) 0xff, (byte) 0xc2,
				0x01, FEATURE_GET_FEATURE_REQUEST, 0 };

		byte [] result = new byte [0];
		
		if (Utils.arrayHasPrefix(commandApdu, expectedHeaderAndLc)) {
			result = getOnlyTagsFromFeatureList(getFeatures());
		} else {
			result = SimulatorManager.getSim().processCommand(commandApdu);
		}

		if (result.length > expectedLength.getAsSignedLong()){
			return new SimplePcscCallResult(PcscConstants.IFD_ERROR_INSUFFICIENT_BUFFER);
		}
		
		return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS,
				result);
		
	}

	private byte[] getOnlyTagsFromFeatureList(List<byte[]> features) {
		byte[] result = new byte[features.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = features.get(i)[0];
		}
		return result;
	}

	private List<byte[]> getFeatures() {
		List<byte[]> featureDefinitions = new ArrayList<byte[]>();
		for (PcscListener listener : connector.getListeners()) {
			if (listener instanceof PcscFeature) {
				featureDefinitions.add(((PcscFeature) listener)
						.getFeatureDefinition());
			}
		}
		return featureDefinitions;
	}

	private PcscCallResult isIccPresent(PcscCallData data) {
		boolean iccPresent = (SimulatorManager.getSim() != null)&& (SimulatorManager.getSim().isRunning());
		
		if (userInterfaces != null) {
			for (VirtualReaderUi ui : userInterfaces) {
				ui.iccPresence(iccPresent);
			}
		}
		
		if (iccPresent) {
			return new SimplePcscCallResult(IFD_ICC_PRESENT);
		}
		return new SimplePcscCallResult(IFD_ICC_NOT_PRESENT);
	}

	private PcscCallResult isIccAbsent(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult listContexts(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult isContextSupported(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult getIfdsp(PcscCallData data) {
		// not supported
		return null;
	}

	private PcscCallResult listInterfaces(PcscCallData data) {
		// not supported
		return null;
	}

	@Override
	public void setConnector(IfdConnector connector) {
		this.connector = connector;
	}

	@Override
	public void setUserInterfaces(List<VirtualReaderUi> newUis) {
		this.userInterfaces = newUis;
	}
}
