package de.persosim.websocket;

import java.util.List;

import de.persosim.driver.connector.UnsignedInteger;
import de.persosim.driver.connector.pcsc.PcscCallResult;
import de.persosim.driver.connector.pcsc.PcscListener;

public class WrappedPcscCallResult implements PcscCallResult {

	private PcscCallResult result;
	private PcscListener sourceListener;

	public WrappedPcscCallResult(PcscCallResult result, PcscListener sourceListener) {
		this.result = result;
		this.sourceListener = sourceListener;
	}

	@Override
	public String getEncoded() {
		return result.getEncoded();
	}

	@Override
	public UnsignedInteger getResponseCode() {
		return result.getResponseCode();
	}

	@Override
	public List<byte[]> getData() {
		return result.getData();
	}
	
	public PcscListener getSourceListener() {
		return sourceListener;
	}

}
