package org.globaltester.logging;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;

import org.globaltester.logging.tags.LogLevel;
import org.globaltester.logging.tags.LogTag;

/**
 * This class is a Logger with basic functionalities.
 * 
 * @author amay
 * 
 */
public final class BasicLogger {

	public static final String ORIGIN_CLASS_TAG_ID = "Originating class";
	public static final String SOURCE_TAG_ID = "Source";
	public static final String EXCEPTION_STACK_TAG_ID = "Exception stack trace";
	public static final String ORIGIN_THREAD_GROUP_TAG_ID = "Originating thread group";
	public static final String LOG_LEVEL_TAG_ID = "Logging level";
	public static final String UI_TAG_ID = "User interface message";
	public static final String TIMESTAMP_TAG_ID = "Timestamp";
	public static final int DEFAULT_LINE_BREAK_THRESHOLD = 3000;

	private static final LogLevel LOGLEVEL_DFLT = LogLevel.DEBUG;
	private static List<LogListener> listeners = new LinkedList<>();

	private static String longLineMarker = "LINETOLONG\t";

	/**
	 * Ensure that this type can not be instantiated
	 */
	private BasicLogger() {
	}

	/**
	 * @see {@link List#add(Object)}
	 * @param newListener
	 * @return
	 */
	public static boolean addLogListener(LogListener newListener) {
		return listeners.add(newListener);
	}

	/**
	 * @see {@link List#remove(Object)}
	 * @param newListener
	 * @return
	 */
	public static boolean removeLogListener(LogListener listener) {
		return listeners.remove(listener);
	}

	public static void log(String messageContent, LogLevel level, LogTag... logTags) {
		messageContent = breakLines(messageContent, DEFAULT_LINE_BREAK_THRESHOLD);

		Message newMessage = new Message(messageContent, logTags);
		newMessage.addLogTag(new LogTag(ORIGIN_CLASS_TAG_ID, getOriginClass()));
		newMessage.addLogTag(new LogTag(ORIGIN_THREAD_GROUP_TAG_ID, Thread.currentThread().getThreadGroup().getName()));
		newMessage.addLogTag(new LogTag(LOG_LEVEL_TAG_ID, level.name()));
		newMessage.addLogTag(new LogTag(TIMESTAMP_TAG_ID, Long.toString(System.currentTimeMillis())));
		for (LogListener curListener : listeners) {
			curListener.log(newMessage);
		}

		// fallback
		if (listeners.isEmpty()) {
			if (level.ordinal() <= LogLevel.ERROR.ordinal()) {
				System.err.println(messageContent); // NOSONAR System.out is valid fallback within logger
			} else {
				System.out.println(messageContent); // NOSONAR System.out is valid fallback within logger
			}
		}

	}

	/**
	 * Find the first external class where the call to the logging methods occured
	 * or return the calling class to this method if no external could be found.
	 * 
	 * @return the originating class name
	 */
	private static String getOriginClass() {
		StackTraceElement[] stack = Thread.currentThread().getStackTrace();
		for (StackTraceElement curElem : stack) {
			if (curElem.getClassName().startsWith("java.")) {
				continue;
			}

			if (curElem.getClassName().startsWith("org.globaltester.logging.")) {
				continue;
			}

			return curElem.getClassName();
		}
		return "unknown origin class";
	}

	/**
	 * Write message to the log, including origin of that message.
	 * 
	 * This method uses LOGLEVEL_DLFT as LogLevel.
	 * 
	 * @param source  origin of this log message
	 * @param message the message to be logged
	 */
	public static void log(InfoSource source, String message) {
		log(source, message, LOGLEVEL_DFLT);
	}

	/**
	 * Write message to the log, including origin of that message.
	 * 
	 * @param source   origin of this log message
	 * @param message  the message to be logged
	 * @param logLevel log level on which the message is shown
	 */
	public static void log(InfoSource source, String message, LogLevel logLevel) {
		log(source.getIDString(), message, logLevel);
	}

	/**
	 * Write message to the log, including originating class of that message.
	 * 
	 * @param className originating class of this log message
	 * @param message   the message to be logged
	 * @param logLevel  log level on which the message is shown
	 */
	public static void log(Class<?> className, String message, LogLevel logLevel) {
		log(className.getCanonicalName(), message, logLevel);
	}

	/**
	 * Write message to the log, including originating class of that message.
	 * 
	 * This method uses LOGLEVEL_DLFT as LogLevel.
	 * 
	 * @param className originating class of this log message
	 * @param message   the message to be logged
	 */
	public static void log(Class<?> className, String message) {
		log(className, message, LOGLEVEL_DFLT);
	}

	/*--------------------------------------------------------------------------------*/

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * This method uses LOGLEVEL_DLFT as LogLevel.
	 * 
	 * @param source origin of this log message
	 * @param e      the Exception to be logged
	 */
	public static void logException(InfoSource source, Throwable e) {
		logException(source, e, LOGLEVEL_DFLT);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * This method uses LOGLEVEL_DLFT as LogLevel.
	 * 
	 * @param source  origin of this log message
	 * @param message the message to be logged
	 * @param e       the Exception to be logged
	 */
	public static void logException(InfoSource source, String message, Throwable e) {
		logException(source, message, e, LOGLEVEL_DFLT);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * @param source   origin of this log message
	 * @param e        the Exception to be logged
	 * @param logLevel log level on which the exception is shown
	 */
	public static void logException(InfoSource source, Throwable e, LogLevel logLevel) {
		logException(source, e.getMessage(), e, logLevel);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * @param source   origin of this log message
	 * @param e        the Exception to be logged
	 * @param logLevel log level on which the exception is shown
	 */
	public static void logException(InfoSource source, String message, Throwable e, LogLevel logLevel) {
		logException(source.getIDString(), message, e, logLevel);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * @param className originating class of this log message
	 * @param e         the Exception to be logged
	 * @param logLevel  log level on which the exception is shown
	 */
	public static void logException(Class<?> className, Throwable e, LogLevel logLevel) {
		logException(className.getCanonicalName(), e.getMessage(), e, logLevel);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * @param className originating class of this log message
	 * @param e         the Exception to be logged
	 * @param logLevel  log level on which the exception is shown
	 */
	public static void logException(Class<?> className, String message, Throwable e, LogLevel logLevel) {
		logException(className.getCanonicalName(), message, e, logLevel);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * @param message  the message to be logged
	 * @param e        the Exception to be logged
	 * @param logLevel log level on which the exception is shown
	 */
	public static void logException(String message, Throwable e, LogLevel logLevel) {
		logException(getOriginClass(), message, e, logLevel);
	}

	/**
	 * Write message to the log, formatted including origin of that message.
	 * 
	 * @param source   originating origin of this log message
	 * @param message  the message to be logged
	 * @param logLevel log level on which the message is shown
	 */
	private static void log(String source, String message, LogLevel logLevel) {
		log(message, logLevel, new LogTag(SOURCE_TAG_ID, source));
	}

	/**
	 * Transform an Exception into user readable form and write it to the log,
	 * including origin of that message.
	 * 
	 * @param source   origin of this log message
	 * @param message  the message to be logged
	 * @param e        Exception to be logged
	 * @param logLevel log level on which the message is shown
	 */
	private static void logException(String source, String message, Throwable e, LogLevel logLevel) {
		StringBuilder sb;

		sb = new StringBuilder();

		sb.append("encountered the following exception: ");
		sb.append(e.getClass().getCanonicalName());

		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			e.printStackTrace(pw);
			sb.append("\n");
			sb.append(sw.toString());
		} catch (IOException e1) { // NOSONAR exception stack trace extraction does not work properly, trying to
									// log this exception will not work either
			sb.append("\n reason: " + e.getMessage());
			sb.append("\n no stack trace output possible because of" + e1.toString());
			e1.printStackTrace(); // NOSONAR System.err is valid fallback within logger
		}

		StackTraceElement[] stackTrace = e.getStackTrace();

		for (StackTraceElement elem : stackTrace) {
			sb.append("\n" + elem.toString());
		}

		log(message, logLevel, new LogTag(SOURCE_TAG_ID, source), new LogTag(EXCEPTION_STACK_TAG_ID, sb.toString()));

	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * This method uses LOGLEVEL_DLFT as LogLevel.
	 * 
	 * @param className originating class of this log message
	 * @param e         the Exception to be logged
	 */
	public static void logException(Class<?> className, Exception e) {
		logException(className, e, LOGLEVEL_DFLT);
	}

	/**
	 * Write exception to the log, including origin of that message.
	 * 
	 * This method uses LOGLEVEL_DLFT as LogLevel.
	 * 
	 * @param className originating class of this log message
	 * @param e         the Exception to be logged
	 */
	public static void logException(Class<?> className, String message, Exception e) {
		logException(className, message, e, LOGLEVEL_DFLT);
	}

	public static String breakLines(String logString, int lineBreakThreshold) {
		if (logString == null) {
			return logString;
		}
		String[] logStringParts = logString.split(System.lineSeparator());
		String result = "";
		for (int i = 0; i < logStringParts.length; i++) {
			String part = logStringParts[i];
			if (part != null && part.length() >= lineBreakThreshold) {
				StringJoiner joiner = new StringJoiner("\n" + longLineMarker);

				while (!part.isEmpty()) {
					joiner.add(part.subSequence(0,
							lineBreakThreshold < part.length() ? lineBreakThreshold : part.length()));
					part = part.substring(
							lineBreakThreshold < part.length() ? lineBreakThreshold : part.length());
				}
				result += joiner.toString();
			} else {
				result += part;
			}
			if (i != logStringParts.length - 1) {
				result += System.lineSeparator();
			}
		}
		return result;

	}

	public static void setLongLineMarker(String longLineMarker) {
		BasicLogger.longLineMarker = longLineMarker;
	}
}
