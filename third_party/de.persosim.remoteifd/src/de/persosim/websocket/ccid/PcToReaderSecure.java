package de.persosim.websocket.ccid;

import java.util.Arrays;

import de.persosim.simulator.exception.NotImplementedException;
import de.persosim.simulator.utils.Utils;

public class PcToReaderSecure {

	private byte[] abData;

	public PcToReaderSecure(byte[] byteArray) {
		if (byteArray[0] != 0x69) {
			throw new IllegalArgumentException("Wrong message type");
		}
		
		short levelParameter = Utils.getShortFromUnsignedByteArray(Arrays.copyOfRange(byteArray, 8, 9));
		
		if (levelParameter == 0) {
			abData = Arrays.copyOfRange(byteArray, 10, byteArray.length);
		} else {
			throw new NotImplementedException("No implementation for split CCID PC_to_RDR_Secure");
		}
	}
	
	public byte [] getAbData() {
		return abData;
	}
	
}
