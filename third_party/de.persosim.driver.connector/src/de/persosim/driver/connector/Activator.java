package de.persosim.driver.connector;

import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator for this bundle.
 * @author mboonk
 *
 */
public class Activator implements BundleActivator {

	private static BundleContext context;
	private static ServiceTracker<DriverConnectorFactory, DriverConnectorFactory> serviceTrackerDriverConnectorFactory;
	private static ServiceRegistration<DriverConnectorFactory> driverConnectorFactoryRegistration;

	public static final String PERSOSIM_CONNECTOR_CONTEXT_ID = "de.persosim";
	public static final String PLUGIN = "de.persosim.driver.connector";
	
	static BundleContext getContext() {
		return context;
	}
	
	public static DriverConnectorFactory getFactory() {
		if (serviceTrackerDriverConnectorFactory != null){
			return serviceTrackerDriverConnectorFactory.getService();
		}
		return null;
	}

	
	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {

		Activator.context = bundleContext;
		
		//register service in service registry
		Hashtable<String, String> props = new Hashtable<String, String>();
		driverConnectorFactoryRegistration = bundleContext.registerService(DriverConnectorFactory.class, new DriverConnectorFactoryImpl(), props);
		
		SimulatorManager.startOsgiAccess(context);
		
		serviceTrackerDriverConnectorFactory = new ServiceTracker<DriverConnectorFactory, DriverConnectorFactory>(context, DriverConnectorFactory.class.getName(), null);
		serviceTrackerDriverConnectorFactory.open();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		Activator.context = null;
		driverConnectorFactoryRegistration.unregister();
		serviceTrackerDriverConnectorFactory.close();
	}

}
