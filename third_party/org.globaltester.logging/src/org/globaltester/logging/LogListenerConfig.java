package org.globaltester.logging;

import org.globaltester.logging.filter.LogFilter;
import org.globaltester.logging.format.LogFormatService;

/**
 * This class is used to configure the filtering and formatting of log messages
 * 
 * @author jkoch
 *
 */
public interface LogListenerConfig {
	
	/**
	 * returns the used filter object which is used by the class
	 * 
	 * @return the LogFilter object
	 */
	public LogFilter getFilter();
	
	/**
	 * returns the used format object which is used by the class
	 * 
	 * @return the LogFormat object
	 */
	public LogFormatService getFormat();
}
