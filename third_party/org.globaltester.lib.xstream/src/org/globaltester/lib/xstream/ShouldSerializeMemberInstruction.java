package org.globaltester.lib.xstream;

public interface ShouldSerializeMemberInstruction {
	
	public static final byte SERIALIZE = 1;
	public static final byte NO_DECISION = 0;
	public static final byte DO_NOT_SERIALIZE = -1;
	
	@SuppressWarnings("rawtypes")
	public byte shouldSerializeMember(Class definedIn, String fieldName);
	
}
