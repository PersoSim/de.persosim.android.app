package de.persosim.driver.connector.exceptions;

import de.persosim.driver.connector.UnsignedInteger;

/**
 * This exception is to be thrown in the event of errors during the PACE bypassing.
 * @author mboonk
 *
 */
public class PaceExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	private UnsignedInteger errorCode;
	private byte [] responseBuffer;
	
	public PaceExecutionException(UnsignedInteger errorCode, byte [] responseBuffer){
		this.errorCode = errorCode;
		this.responseBuffer = responseBuffer;
	}
	
	/**
	 * @return the PCSC error code
	 */
	public UnsignedInteger getError(){
		return errorCode;
	}
	
	/**
	 * @return the response to be sent
	 */
	public byte [] getResponseBuffer(){
		return responseBuffer;
	}
}
