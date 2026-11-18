package org.globaltester.logging.filter;

import org.globaltester.logging.Message;

/**
 * Simple filter that just always returns true which means that the log entry
 * should be logged
 * 
 * @author jkoch
 *
 */
public class NullFilter implements LogFilter {

	@Override
	public boolean matches(Message msg) {
		return true;
	}

}
