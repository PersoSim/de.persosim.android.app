package org.globaltester.simulator.event;

public class DecodedCommandApduEvent extends ApduEvent {

	public DecodedCommandApduEvent(byte[] byteArray) {
		super(byteArray);
	}

}
