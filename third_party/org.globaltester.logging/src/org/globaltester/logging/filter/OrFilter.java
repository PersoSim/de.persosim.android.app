package org.globaltester.logging.filter;

import org.globaltester.logging.Message;

/**
 * Filtering evaluates as logical OR of the results of all containes filters.
 * 
 * During evaluation of the filter result all filters are evaluated regardless
 * of the intermediate result (no shortcut evaluation). This ensures that all
 * encapsulated filters are notified of all available messages (e.g. in order to
 * track progress).
 * 
 * @author jkoch
 *
 */
public class OrFilter implements LogFilter {

	private LogFilter[] filters;

	public OrFilter(LogFilter... filters) {
		this.filters = filters;
	}

	@Override
	public boolean matches(Message msg) {

		boolean log = false;

		for (LogFilter filter : filters) {
			// evaluate current filter
			log |= filter.matches(msg);
		}		
		return log;
	}
	
}
