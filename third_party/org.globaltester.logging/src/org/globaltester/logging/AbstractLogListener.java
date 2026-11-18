package org.globaltester.logging;

/**
 * The class provides a standard {@link #logged(LogEntry)} method that formats
 * and filters incoming log entries. Those entries can be printed with
 * {@link #displayLogMessage(String)}.
 * 
 * @author amay
 *
 */
public abstract class AbstractLogListener implements LogListener {
	private LogListenerConfig config = new LogListenerConfigImpl();
	
	@Override
	public void log(Message msg) {
		if (config.getFilter().matches(msg)) {
			// format the entry
			String formattedMsg = config.getFormat().format(msg);
			displayLogMessage(formattedMsg);
		}
	}
	
	public LogListenerConfig getLrc() {
		return config;
	}

	public void setConfig(LogListenerConfig lrc) {
		this.config = lrc;
	}

	/**
	 * is used to print a filtered and formatted log entry to a specific target
	 * 
	 * @param msg
	 *            the log entry to print
	 */
	public abstract void displayLogMessage(String msg);
}
