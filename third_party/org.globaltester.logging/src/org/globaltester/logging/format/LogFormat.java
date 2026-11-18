package org.globaltester.logging.format;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import org.globaltester.logging.BasicLogger;
import org.globaltester.logging.Message;
import org.globaltester.logging.tags.LogTag;

/**
 * Formats the @{link Message}. It is used to format or manipulate log messages.
 * @author amay
 */
public class LogFormat implements LogFormatService {

	@Override
	public String format(Message msg) {
		return "["
				+ getLogLevel(msg) + " - "
				+ getTimestamp(msg) + "] "
				+ msg.getMessageContent();
	}

	/**
	 * Check if the tag has the given ID and if so extract the first additional data string.
	 * 
	 * @param curTag tag to be checked
	 * @param id 
	 * @return the first additional data string if available and the ID matches the tag, else null 
	 */
	public static String extractTag(Message msg, String id) {
		for (LogTag curTag : msg.getLogTags()) {
			if (id.equals(curTag.getId()) && curTag.getAdditionalData().length >= 1) {
				return curTag.getAdditionalData()[0];
			}
		}
		return null;
	}
	
	public static String getTimestamp(Message msg) {
		String tagValue = extractTag(msg, BasicLogger.TIMESTAMP_TAG_ID);
		if (tagValue != null) {
			return DateTimeFormatter.ISO_DATE_TIME.withZone(ZoneOffset.systemDefault()).format(Instant.ofEpochMilli(Long.valueOf(tagValue)));
		} else {
			return "";
		}
	}

	public static String getLogLevel(Message msg){
		String tagValue = extractTag(msg, BasicLogger.LOG_LEVEL_TAG_ID);
		return tagValue != null ? tagValue : "UNKNOWN";
	}
}
