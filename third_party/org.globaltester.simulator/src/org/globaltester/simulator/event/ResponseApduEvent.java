package org.globaltester.simulator.event;

public class ResponseApduEvent extends ApduEvent {

	public ResponseApduEvent(byte[] byteArray) {
		super(byteArray);
	}

}
