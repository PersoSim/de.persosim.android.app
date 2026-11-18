package org.globaltester.logging.format;

import org.globaltester.logging.Message;

/**
 * "Formats" the {@link Message} to consist only of the message content.
 * @author amay
 */
public class NullFormat implements LogFormatService{	
	@Override
	public String format(Message msg) {
		return msg.getMessageContent();
	}
}
