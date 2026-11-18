package de.persosim.driver.connector.pcsc;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.simulator.utils.Utils;

/**
 * This class is the parent for all {@link PcscFeature} implementations. It
 * provides the mechanism for setting a control code at instantiation time to
 * prevent clashes by arbitrary chosen control codes.
 * 
 * @author mboonk
 *
 */
public abstract class AbstractPcscFeature implements PcscFeature {
	private UnsignedInteger controlCode;
	private byte featureTag;

	public AbstractPcscFeature(UnsignedInteger controlCode, byte featureTag) {
		this.controlCode = controlCode;
		this.featureTag = featureTag;
	}

	/**
	 * @return the controlCode
	 */
	public UnsignedInteger getControlCode() {
		return controlCode;
	}
	
	@Override
	public byte[] getFeatureDefinition() {
		return Utils.concatByteArrays(new byte [] {featureTag, 4}, controlCode.getAsByteArray());
	}
}
