package org.globaltester.logging.format;

import org.globaltester.logging.Message;

/**
 * Service for formatting log messages
 * 
 * @author jkoch
 *
 */
public interface LogFormatService {
	
	/**
	 * returns a formatted String representing the {@link Message} object. 
	 * 
	 * @param msg
	 *            	the {@link Message} object to be formatted
	 * @return the formatted log message as a String
	 */
	public String format(Message msg);

}
