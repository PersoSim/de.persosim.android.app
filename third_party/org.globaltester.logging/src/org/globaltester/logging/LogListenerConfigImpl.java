package org.globaltester.logging;

import org.globaltester.logging.filter.LogFilter;
import org.globaltester.logging.filter.NullFilter;
import org.globaltester.logging.format.LogFormat;
import org.globaltester.logging.format.LogFormatService;

/**
 * This implementation of LogListenerConfig provides a format object and a
 * simple NullFilter that accepts all log entries.
 * 
 * @author jkoch
 *
 */
public class LogListenerConfigImpl implements LogListenerConfig{
	
	public LogFormat format = new LogFormat();
	public LogFilter filter = new NullFilter();
	
	@Override
	public LogFilter getFilter() {
		return filter;
	}
	
	@Override
	public LogFormatService getFormat() {
		return format;
	}
}
