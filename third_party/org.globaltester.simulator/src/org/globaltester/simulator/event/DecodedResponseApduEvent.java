package org.globaltester.simulator.event;

public class DecodedResponseApduEvent extends ApduEvent {

	public DecodedResponseApduEvent(byte[] byteArray) {
		super(byteArray);
	}

}
