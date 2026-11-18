package org.globaltester.logging.filter;

import java.util.Arrays;
import java.util.List;

import org.globaltester.logging.Message;
import org.globaltester.logging.tags.LogTag;

public class TagFilter implements LogFilter {

	private String logTagId;
	private String[] logTagData;

	// Constructor
	public TagFilter(String logTagId, String... data) {
		this.logTagId = logTagId;
		this.logTagData = data;
	}
	
	@Override
	public boolean matches(Message msg) {
		if (msg != null) {
			for (LogTag curTag : msg.getLogTags()) {
				if (curTag.getId().equals(logTagId)) {
					return checkTagForData(curTag);
				}
			}
		}
		return false;
	}

	boolean checkTagForData(LogTag tag) {
		if (logTagData.length == 0) return true;
		
		List<String> actualTagData = Arrays.asList(tag.getAdditionalData());
		for (String current : logTagData) {
			if (actualTagData.contains(current)) {
				return true;
			}
		}
		
		
		return false;
	}

}
