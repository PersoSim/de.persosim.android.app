package org.globaltester.simulator.event;

public class ApduEvent extends SimulatorEvent {

	private byte[] bytes;

	public ApduEvent(byte[] byteArray) {
		bytes = byteArray;
	}

	public byte[] getBytes() {
		return bytes;
	}

}
