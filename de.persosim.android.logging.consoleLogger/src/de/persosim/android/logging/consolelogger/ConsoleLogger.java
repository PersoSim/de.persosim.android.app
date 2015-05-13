package de.persosim.android.logging.consolelogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;

import org.globaltester.logging.AbstractLogListener;
import org.globaltester.logging.LogListenerConfig;
import org.globaltester.logging.filter.AndFilter;
import org.globaltester.logging.filter.BundleFilter;
import org.globaltester.logging.filter.LevelFilter;
import org.globaltester.logging.filter.LogFilter;
import org.globaltester.logging.format.LogFormat;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.util.tracker.ServiceTracker;

import de.persosim.android.logging.Configuration;
import android.app.Service;
import android.content.Context;
import android.util.Log;

/**
 * This {@link LogListener} implementation writes all {@link LogEntry}s to the central {@link OsgiService} of the Android app.
 * @author slutters
 *
 */
public class ConsoleLogger extends AbstractLogListener implements Configuration {
	
	private static final String LOG_TAG = ConsoleLogger.class.getName();
	private static final String METHOD_LOG = "addToLog";
	
	public static final byte STD_LEVELS[] ={1,2,3,4,5,6};
	
    private ServiceTracker<?, ?> serviceTracker;
    private Service osgiWrapperService;
    private Method logMethod;

	DateFormat format = DateFormat.getDateTimeInstance();
	
	BundleContext bundleContext;
	
	public ConsoleLogger(BundleContext bundleContext) {
		super();
		
		this.bundleContext = bundleContext;
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

	@Override
	public void displayLogMessage(String msg) {
		Log.d(LOG_TAG, "START logged(LogEntry)");
		
		if (msg == null){
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
						logMethod.invoke(osgiWrapperService, msg);
						Log.d(LOG_TAG, "successfully executed method " + METHOD_LOG);
					} catch (IllegalAccessException
							| IllegalArgumentException
							| InvocationTargetException e) {
						Log.w(LOG_TAG, "failed to execute method " + METHOD_LOG);
					}
				}
			}
		}
		
		Log.d(LOG_TAG, "END logged(LogEntry)");
	}
	
	public void setLogLevel(byte logLevel) {
		Log.d(LOG_TAG, "START setLogLevel(byte)");
		
		Log.d(LOG_TAG, "set log level to: " + logLevel);
		
		LogListenerConfig lrc = makeConfig(logLevel);
		setLrc(lrc);
		
		Log.d(LOG_TAG, "END setLogLevel(byte)");
	}
	
	private static byte[] getLogLevels(byte logLevel) {
		byte[] logLevels;

		switch (logLevel) {
		case 1:
			logLevels = new byte[] { 1, 2, 3, 4, 5, 6 };
			break;
		case 2:
			logLevels = new byte[] { 2, 3, 4, 5, 6 };
			break;
		case 3:
			logLevels = new byte[] { 3, 4, 5, 6 };
			break;
		case 4:
			logLevels = new byte[] { 4, 5, 6 };
			break;
		case 5:
			logLevels = new byte[] { 5, 6 };
			break;
		case 6:
			logLevels = new byte[] { 6 };
			break;
		default:
			logLevels = new byte[] { 1, 2, 3, 4, 5, 6, 120 };
			break;
		}

		return logLevels;
	}
	
	private static LogListenerConfig makeConfig(byte logLevel) {
		final byte logLvl = logLevel;
		
		LogListenerConfig lrc = new LogListenerConfig() {
			byte logLevels [] = getLogLevels(logLvl);
			String bundleList [] = {"de.persosim"};
			
			public LogFormat format = new LogFormat();
			public BundleFilter bundleFilter = new BundleFilter(bundleList);
			public LevelFilter levelFilter = new LevelFilter(logLevels);
			public LogFilter [] filters = {bundleFilter, levelFilter};	
			public AndFilter filter = new AndFilter(filters);
			
			@Override
			public LogFilter getFilter() {
				return filter;
			}

			@Override
			public LogFormat getFormat() {
				return format;
			}
		};
		
		return lrc;
	}

}
