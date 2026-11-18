package org.globaltester.logging.tags;

import java.util.Arrays;

/**
 * This enum presents the usable log levels for logging in the Global Tester
 * universe. The absolute ordinals of this enum are subject to change and not to
 * be relied upon, however the order of the enum entries is relevant. The
 * entries are ordered by verbosity.
 * 
 * @author mboonk
 *
 */
public enum LogLevel {

	FATAL, ERROR, WARN, INFO, DEBUG, TRACE;

	public static LogLevel[] getUpTo(LogLevel level) {
		return Arrays.copyOf(values(), level.ordinal() + 1);
	}

	public static String[] getUpToAsNames(LogLevel level) {
		LogLevel [] levels = getUpTo(level);
		String [] result = new String [levels.length];
		for (int i = 0; i < levels.length; i++){
			result[i] = levels[i].name();
		}
		return result;
	}

}
