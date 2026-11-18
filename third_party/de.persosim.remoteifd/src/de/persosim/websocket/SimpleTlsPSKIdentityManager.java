package de.persosim.websocket;

import org.bouncycastle.tls.TlsPSKIdentityManager;

public class SimpleTlsPSKIdentityManager implements TlsPSKIdentityManager {

	byte [] psk;
	
	public SimpleTlsPSKIdentityManager(byte [] psk) {
		this.psk = psk;
	}
	
	@Override
	public byte[] getHint() {
		return new byte [0];
	}

	@Override
	public byte[] getPSK(byte[] identity) {
		return psk.clone();
	}

}
