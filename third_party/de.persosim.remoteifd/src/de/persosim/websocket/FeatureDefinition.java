package de.persosim.websocket;

import java.util.Arrays;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.simulator.utils.Utils;

public class FeatureDefinition {
	private byte description;
	private UnsignedInteger controlCode;

	public FeatureDefinition(byte [] encoded) {
		this.description = encoded[0];
		this.controlCode = new UnsignedInteger(Arrays.copyOfRange(encoded, 2, encoded.length));
	}

	public FeatureDefinition(byte description, UnsignedInteger controlCode) {
		this.description = description;
		this.controlCode = controlCode;
	}

	public byte getDescription() {
		return description;
	}

	public UnsignedInteger getControlCode() {
		return controlCode;
	}
	
	public byte [] getEncoded () {
		// Description byte|length field with value 4|control code
		return Utils.concatByteArrays(new byte [] {description, 4}, controlCode.getAsByteArray());
	}
}
