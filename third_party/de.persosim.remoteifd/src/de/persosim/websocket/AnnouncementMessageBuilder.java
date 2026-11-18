package de.persosim.websocket;

public interface AnnouncementMessageBuilder {
	/**
	 * This builds a message to be send by the announcement server
	 * 
	 * @return the announcement message
	 */
	byte[] build();
}