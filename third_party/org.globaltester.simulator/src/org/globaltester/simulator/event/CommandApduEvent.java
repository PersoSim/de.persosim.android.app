package org.globaltester.simulator.event;

public class CommandApduEvent extends ApduEvent {

	public CommandApduEvent(byte[] byteArray) {
		super(byteArray);
	}

}
