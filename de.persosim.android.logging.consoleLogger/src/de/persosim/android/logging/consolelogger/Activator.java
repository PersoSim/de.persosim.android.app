package de.persosim.android.logging.consolelogger;

import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The activator class controls the plug-in life cycle and manages the tracking
 * of the {@link LogReaderService}.
 */
public class Activator implements BundleActivator {
	
	private BundleContext bundleContext;
	
	private LinkedList<LogReaderService> readers = new LinkedList<>();
	private ConsoleLogger consoleLogger;
	private ServiceTracker<LogReaderService, LogReaderService> logReaderTracker;
	
	// This will be used to keep track of listeners as they are un/registering
	private ServiceListener serviceListener;
	
	/**
	 * The constructor
	 */
	public Activator() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		
		bundleContext = context;
		
		consoleLogger = new ConsoleLogger(bundleContext);
		
		//register service in service registry
		Hashtable<String, String> props = new Hashtable<String, String>();
		bundleContext.registerService(ConsoleLogger.class, consoleLogger, props);
		
		serviceListener = new ServiceListener() {
			@Override
			public void serviceChanged(ServiceEvent event) {
				BundleContext bundleContext = event.getServiceReference().getBundle().getBundleContext();
				LogReaderService readerService = (LogReaderService) bundleContext.getService(event.getServiceReference());
				if (readerService != null){
					if (event.getType() == ServiceEvent.REGISTERED){
						readers.add(readerService);
						readerService.addLogListener(consoleLogger);
					} else if (event.getType() == ServiceEvent.UNREGISTERING){
						readerService.removeLogListener(consoleLogger);
						readers.remove(readerService);
					}
				}
			}
		};
		
		logReaderTracker = new ServiceTracker<>(bundleContext, LogReaderService.class.getName(), null);
		logReaderTracker.open();
		Object[] readers = logReaderTracker.getServices();
		if (readers != null){
			for (int i=0; i<readers.length; i++){
				LogReaderService readerService = (LogReaderService) readers [i];
				this.readers.add(readerService);
				readerService.addLogListener(consoleLogger);
			}
		}
		
        String filter = "(objectclass=" + LogReaderService.class.getName() + ")";
        try {
        	bundleContext.addServiceListener(serviceListener, filter);
        } catch (InvalidSyntaxException e) {
            e.printStackTrace();
        }

	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		Iterator<LogReaderService> iterator = readers.iterator();
        while (iterator.hasNext())
        {
            LogReaderService readerService = iterator.next();
            readerService.removeLogListener(consoleLogger);
            iterator.remove();
        }
		logReaderTracker.close();

	}

}
