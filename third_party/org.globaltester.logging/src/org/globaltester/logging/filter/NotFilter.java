package org.globaltester.logging.filter;

import org.globaltester.logging.Message;

/**
 * Filtering evaluates as logical NOT of the results of all contained filters.
 * 
 * During evaluation of the filter result all filters are evaluated regardless
 * of the intermediate result (no shortcut evaluation). This ensures that all
 * encapsulated filters are notified of all available messages (e.g. in order to
 * track progress).
 * 
 * @author jkoch
 *
 */
public class NotFilter implements LogFilter{
	
	private LogFilter[] filters;

	public NotFilter(LogFilter... filters) {
		this.filters = filters;
	}
	
	@Override
	public boolean matches(Message msg) {
		
		boolean log = true;

		for (LogFilter filter : filters) {
			// evaluate current filter
			log &= !filter.matches(msg);
		}		
		return log;
	}

}
