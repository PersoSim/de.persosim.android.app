package de.persosim.driver.connector.pcsc;

import java.util.Arrays;
import java.util.List;

import de.persosim.driver.connector.IfdInterface;
import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.simulator.utils.HexString;

/**
 * This {@link PcscCallResult} contains a status code and optional data parameters.
 * @author mboonk
 *
 */
public class SimplePcscCallResult implements PcscCallResult {
	private String resultEncoding;
	private UnsignedInteger responseCode;
	private List<byte []> data;
	
	public SimplePcscCallResult(UnsignedInteger responseCode){
		resultEncoding = responseCode.getAsHexString();
		this.responseCode = responseCode;
	}
	
	public SimplePcscCallResult(UnsignedInteger responseCode, byte [] ... resultData) {
		this(responseCode);
		this.data = Arrays.asList(resultData);
		for (byte[] current : data) {
			resultEncoding += IfdInterface.MESSAGE_DIVIDER + HexString.encode(current);
		}
	}
	
	@Override
	public String getEncoded() {
		return resultEncoding;
	}

	@Override
	public UnsignedInteger getResponseCode() {
		return responseCode;
	}

	@Override
	public List<byte[]> getData() {
		return data;
	}

}
