package org.globaltester.logging;

/**
 * Interface for entities that want to be notified for new log Messages.
 * These can be registered in BasicLogger.
 * 
 * @author may.alexander
 *
 */
public interface LogListener {

	/**
	 * This method will be called with every new lMessage that is logged. 
	 * @param msg the logged message
	 */
	void log(Message msg);

}
