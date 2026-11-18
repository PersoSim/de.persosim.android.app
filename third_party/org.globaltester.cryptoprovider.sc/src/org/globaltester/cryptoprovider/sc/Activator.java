package org.globaltester.cryptoprovider.sc;

import org.globaltester.cryptoprovider.Cryptoprovider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {

	private static BundleContext context;

	static BundleContext getContext() {
		return context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext bundleContext) throws Exception {
		System.out.println("START Activator sc");
		Activator.context = bundleContext;
		
		String str = Cryptoprovider.class.getName();
		System.out.println("Cryptoprovider sc: "+str);
		
		//register service in service registry
		Cryptoprovider cryptoProvider = new ProviderSc();
		bundleContext.registerService(Cryptoprovider.class, cryptoProvider, cryptoProvider.getProperties());
		System.out.println("END Activator sc");
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
	}

}
