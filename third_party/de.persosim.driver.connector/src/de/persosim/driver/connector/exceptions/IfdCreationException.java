package de.persosim.driver.connector.exceptions;

import java.io.IOException;

public class IfdCreationException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public IfdCreationException(String string, IOException e) {
		super(string,e);
	}

}
