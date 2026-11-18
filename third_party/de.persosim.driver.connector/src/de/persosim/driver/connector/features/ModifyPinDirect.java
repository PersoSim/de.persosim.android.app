package de.persosim.driver.connector.features;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.tags.LogLevel;

import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.VirtualReaderUi;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscConstants;
import de.persosim.driver.connector.pcsc.SimplePcscCallResult;
import de.persosim.driver.connector.pcsc.UiEnabled;
import de.persosim.simulator.utils.HexString;
import de.persosim.simulator.utils.Utils;

public class ModifyPinDirect extends AbstractPcscFeature implements UiEnabled {

	public static final byte FEATURE_TAG = 0x07;

	private List<VirtualReaderUi> interfaces;

	public ModifyPinDirect(UnsignedInteger controlCode) {
		super(controlCode, FEATURE_TAG);
	}

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		if (data.getFunction().equals(IfdInterface.PCSC_FUNCTION_DEVICE_CONTROL)) {
			if (Arrays.equals(data.getParameters().get(0), getControlCode().getAsByteArray())) {
				List<byte[]> params = data.getParameters();
				byte[] apduHeaderExpected = HexString.toByteArray("002C0203");
				byte[] expectedLength = params.get(2);

				byte[] resetRetryCounterApduBytes;

				byte[] newPin1, newPin2;
				boolean match;

				for (VirtualReaderUi virtualReaderUi : interfaces) {
					match = false;

					while (!match) {
						virtualReaderUi.display("Please enter new PIN");
						try {
							newPin1 = virtualReaderUi.getPin();
						} catch (IOException e) {
							newPin1 = null;
						}

						if (newPin1 == null) {
							return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, new byte[] { 0x64, 0x01 });
						}

						virtualReaderUi.display("Please re-enter new PIN");
						try {
							newPin2 = virtualReaderUi.getPin();
						} catch (IOException e) {
							newPin2 = null;
						}

						if (newPin2 == null) {
							return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, new byte[] { 0x64, 0x01 });
						}

						match = Arrays.equals(newPin1, newPin2) && (newPin1 != null);

						if (match) {
							List<byte[]> list = new ArrayList<>(2);

							resetRetryCounterApduBytes = Utils.concatByteArrays(apduHeaderExpected,
									HexString.toByteArray(HexString.hexifyByte(newPin1.length)), newPin1);

							list.add(resetRetryCounterApduBytes);
							list.add(expectedLength);

							data.setFunction(IfdInterface.PCSC_FUNCTION_TRANSMIT_TO_ICC);
							data.setParameters(list);

							BasicLogger.log(getClass(), "modified param 0 for pin modification: " + HexString.encode(data.getParameters().get(0)), LogLevel.INFO);
						} else {
							virtualReaderUi.display("new PIN does not match");
							return new SimplePcscCallResult(PcscConstants.IFD_SUCCESS, new byte[] { 0x64, 0x02 });
						}
					}
				}
			}
		}
		return null;
	}

	@Override
	public void setUserInterfaces(List<VirtualReaderUi> interfaces) {
		this.interfaces = interfaces;
	}

}
