package org.globaltester.logging;

import java.util.ArrayList;
import java.util.List;

import org.globaltester.logging.tags.LogTag;

public class Message {
	
	protected String messageContent;

	protected List<LogTag> messageTags;
	
	public Message(String messageContent, LogTag... logTags) {
		this.messageContent = messageContent;
		
		messageTags = new ArrayList<>();
		addLogTag(logTags);
	}
	
	public String getMessageContent() {
		return messageContent;
	}
	
	public List<LogTag> getLogTags(){
		return new ArrayList<LogTag>(messageTags);
	}
	
	public void addLogTag(LogTag... logTags) {
		for(LogTag logTag : logTags) {
			messageTags.add(logTag);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messageContent == null) ? 0 : messageContent.hashCode());
		result = prime * result + ((messageTags == null) ? 0 : messageTags.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Message other = (Message) obj;
		
		if (messageContent == null) {
			if (other.messageContent != null)
				return false;
		} else if (!messageContent.equals(other.messageContent)) {
			return false;
		} else {
			//messageContent fields are equal
		}
		
		if (messageTags == null) {
			if (other.messageTags != null)
				return false;
		} else if (!messageTags.equals(other.messageTags)) {
			return false;
		} else {
			//messageTags fields are equal
		}
		
		return true;
	}
	
}
