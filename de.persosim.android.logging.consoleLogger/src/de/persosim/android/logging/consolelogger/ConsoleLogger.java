package de.persosim.android.logging.consolelogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;

import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.util.tracker.ServiceTracker;

import android.app.Service;
import android.content.Context;
import android.util.Log;

/**
 * This {@link LogListener} implementation writes all {@link LogEntry}s to the central {@link OsgiService} of the Android app.
 * @author slutters
 *
 */
public class ConsoleLogger implements LogListener {
	
	private static final String LOG_TAG = ConsoleLogger.class.getName();
	private static final String METHOD_LOG = "addToLog";
	
    private ServiceTracker<?, ?> serviceTracker;
    private Service osgiWrapperService;
    private Method logMethod;

	DateFormat format = DateFormat.getDateTimeInstance();
	
	BundleContext bundleContext;
	
	public ConsoleLogger(BundleContext bundleContext) {
		super();
		
		this.bundleContext = bundleContext;
	}
	
	@Override
	public void logged(LogEntry entry) {
		Log.d(LOG_TAG, "START logged(LogEntry)");
		
		if(entry == null) {
			Log.d(LOG_TAG, "log entry is null");
		} else {
			String message = entry.getMessage();
			if (message == null){
				Log.d(LOG_TAG, "message is null");
			} else {
				if(osgiWrapperService == null) {
					Log.d(LOG_TAG, "retrieving OSGI service");
					osgiWrapperService = getOsgiWrapperService();
				} else {
					Log.d(LOG_TAG, "OSGI service already retrieved");
				}
				
				if(osgiWrapperService != null) {
					if(logMethod == null) {
						try {
							logMethod = osgiWrapperService.getClass().getMethod(METHOD_LOG, String.class);
							Log.d(LOG_TAG, "successfully loaded method " + METHOD_LOG);
						} catch (NoSuchMethodException e) {
							Log.w(LOG_TAG, "failed to load method " + METHOD_LOG);
						}
					}
					
					if(logMethod != null) {
						try {
							logMethod.invoke(osgiWrapperService, message);
							Log.d(LOG_TAG, "successfully executed method " + METHOD_LOG);
						} catch (IllegalAccessException
								| IllegalArgumentException
								| InvocationTargetException e) {
							Log.w(LOG_TAG, "failed to execute method " + METHOD_LOG);
						}
					}
				}
			}
		}
		
		Log.d(LOG_TAG, "END logged(LogEntry)");
	}
	
	/**
	 * This method returns the OSGI wrapper service currently registered in the bundle registry.
	 * @return the OSGI wrapper service currently registered in the bundle registry, otherwise null
	 */
	private Service getOsgiWrapperService() {
		Log.d(LOG_TAG, "START getOsgiBundle()");
		
		Service service = osgiWrapperService;
		
		serviceTracker = new ServiceTracker<>(bundleContext, Context.class, null);
		serviceTracker.open();
		
		try {
			Log.d(LOG_TAG, "waiting for Android service object");
			Object serviceObject = serviceTracker.waitForService(10000);
			service = (Service) serviceObject;
			Log.d(LOG_TAG, "Android service object is: " + service);
			String appName = service.getApplicationContext().getPackageName();
			Log.d(LOG_TAG, "app name is: " + appName);
		} catch (InterruptedException e) {
			Log.d(LOG_TAG, "waiting for Android service object interrupted");
		} catch (IllegalArgumentException e) {
			Log.d(LOG_TAG, "waiting for Android service object timed out");
		}
		
		Log.d(LOG_TAG, "END getOsgiBundle()");
		
		return service;
	}

}
