package de.persosim.driver.connector.pcsc;

import java.util.List;

import de.persosim.driver.connector.UnsignedInteger;

/**
 * This interface describes the result data for a PCSC function call.
 * 
 * @author mboonk
 * 
 */
public interface PcscCallResult {
	/**
	 * The format used for this response is:
	 * RESPONSECODE|[data]
	 * <br/>
	 * [data] consists of a concatenation of hex encoded byte arrays divided by |
	 * 
	 * @return the data encoded for sending to the native driver
	 */
	public String getEncoded();
	
	public UnsignedInteger getResponseCode();
	
	public List<byte []> getData();
}
