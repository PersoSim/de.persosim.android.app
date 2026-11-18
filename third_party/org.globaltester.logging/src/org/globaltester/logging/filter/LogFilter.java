package org.globaltester.logging.filter;

import org.globaltester.logging.Message;

/**
 * Service for filtering log messages
 * 
 * @author jkoch
 *
 */
public interface LogFilter {
	
	/**
	 * Filters a {@link Message} object and return true or false if the {@link Message} object
	 * doesn't fit the requirements
	 * 
	 * @param msg
	 *            the LogEntry object which contains the message to log
	 *            
	 * @return 'true' iff the entry fits the criteria and should be logged.
	 */
	public boolean matches(Message msg);

}
