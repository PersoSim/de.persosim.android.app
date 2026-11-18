package org.globaltester.cryptoprovider;

import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This class is intended to be used as source for the CryptoProvider in order
 * to allow using the code base as single source solution when porting to
 * Android.
 * 
 * @author amay, mboonk, jkoch
 * 
 */
public class Crypto {
	
	private static ServiceTracker<Cryptoprovider, Cryptoprovider> serviceTrackerCrypto = null;
	private static Filter filter = null;
	private static Crypto instance;
	
	/**
	 * Singleton constructor, ensures that the class can not be instantiated from outside.
	 */
	private Crypto(){
	};
	
	/**
	 * @return singleton instance
	 */
	public static Crypto getInstance() {
		if (instance == null) {
			instance = new Crypto();
		}
		return instance;
	}
	
	
	// this variable is solely used for manual overrides
	private static Provider providerObject = null;
	
	/**
	 * WARNING: Manual override only, improper use may result in undesired
	 * behavior!
	 * 
	 * This method manually overrides the {@link Provider} to be returned by
	 * {@link Crypto#getCryptoProvider()} in the future.
	 * Call method with argument "null" to disable manual override.
	 * 
	 * @param newProvider
	 */
	public static void setCryptoProvider(Provider newProvider) {
		providerObject = newProvider;
	}
	
	/**
	 * Returns a {@link Provider} without any additional requirements
	 * @return a {@link Provider} without any additional requirements
	 */
	public static Provider getCryptoProvider() {
		String filterString = "(" + Constants.OBJECTCLASS + "=" + Cryptoprovider.class.getName() + ")";
		
		return getCryptoProvider(filterString);
	}
	
	/**
	 * Returns a {@link Provider} wit additional requirements as specified by the provided filter String.
	 * Filter String must comply to RFC1960.
	 * @param filterString the RFC1960 compliant String to filter for
	 * @return a {@link Provider} wit additional requirements as specified by the provided String
	 */
	public static Provider getCryptoProvider(String filterString) {
		if (providerObject != null) {
			return providerObject;
		}
		
		Filter newFilter = null;
		
		BundleContext bc = Activator.getContext();
		
		if (bc != null) {
			try {
				newFilter = bc.createFilter(filterString);
			} catch (InvalidSyntaxException e) {
				return null;
			}
		}
		
		return getInstance().getCryptoProviderFromService(newFilter);
	}
	
	private Provider getCryptoProviderFromService(Filter newFilter) {
		if((filter != null) && (!filter.equals(newFilter)) && (serviceTrackerCrypto != null)) {
			serviceTrackerCrypto.close();
			serviceTrackerCrypto = null;
		}
		
		if ((serviceTrackerCrypto == null) && (Activator.getContext() != null)){
			serviceTrackerCrypto = new ServiceTracker<Cryptoprovider, Cryptoprovider>(Activator.getContext(), newFilter, null);
			serviceTrackerCrypto.open();
			filter = newFilter;
		}
		
		if (serviceTrackerCrypto != null){
			Cryptoprovider service = (Cryptoprovider) serviceTrackerCrypto.getService();
			
			if (service != null) {
				return service.getCryptoProviderObject();
			}
		}
		
		// use default system provider as fallback
		Provider[] providers = Security.getProviders();
		if (providers != null && providers.length > 0) {
			return providers[0];
		}
		
		// if everything fails
		return null;
	}

	private static SecureRandom secureRandom = null;
	
	public static SecureRandom getSecureRandom() {
		if (secureRandom != null) return secureRandom;
		return new SecureRandom();
	}
	
	public static void setSecureRandom(SecureRandom newRandom) {
		secureRandom = newRandom;
	}
}
