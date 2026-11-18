package de.persosim.driver.connector.features;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.pcsc.AbstractPcscFeature;
import de.persosim.driver.connector.pcsc.PcscCallData;
import de.persosim.driver.connector.pcsc.PcscCallResult;

public class VerifyPinDirect extends AbstractPcscFeature {

	public static final byte FEATURE_TAG = 0x06;
	
	public VerifyPinDirect(UnsignedInteger controlCode) {
		super(controlCode, FEATURE_TAG);
	}

	@Override
	public PcscCallResult processPcscCall(PcscCallData data) {
		return null;
	}

}
