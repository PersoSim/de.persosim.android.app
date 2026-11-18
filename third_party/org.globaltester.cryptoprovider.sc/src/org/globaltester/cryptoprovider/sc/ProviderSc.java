package org.globaltester.cryptoprovider.sc;

import java.security.Provider;
import java.security.Security;
import java.util.Hashtable;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.globaltester.cryptoprovider.Cryptoprovider;

public class ProviderSc implements Cryptoprovider {
	
	private BouncyCastleProvider provider;
	
	public ProviderSc() {
		 provider = new BouncyCastleProvider();
		 Security.addProvider(provider);
	}
	
	@Override
	public String getCryptoProviderString() {
		return provider.getName(); //returns "SC"
	}
	
	@Override
	public Provider getCryptoProviderObject() {
		return provider;
	}
	
	@Override
	public Hashtable<String, String> getProperties() {
		Hashtable<String, String> props = new Hashtable<String, String>();
		props.put(Cryptoprovider.NAME, getCryptoProviderObject().getName());
		props.put(Cryptoprovider.VERSION, String.valueOf(getCryptoProviderObject().getVersion()));
		props.put(Cryptoprovider.INFO, getCryptoProviderObject().getInfo());
		return props;
	}
	
}
