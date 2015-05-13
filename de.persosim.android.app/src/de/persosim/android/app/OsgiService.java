package de.persosim.android.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.apache.felix.framework.Felix;
import org.apache.felix.framework.util.FelixConstants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;



public class OsgiService extends Service {
	
	public static final String LOG_TAG = OsgiService.class.getName();
	public static final String OSGISERVICE = LOG_TAG;
	public static final String OSGISERVICE_STATUS = LOG_TAG + ".status";
	public static final String OSGISERVICE_STATUS_MESSAGE = OSGISERVICE_STATUS + ".message";
	public static final String OSGISERVICE_START = LOG_TAG + ".start";
	public static final String OSGISERVICE_BUNDLE_NAME = LOG_TAG + ".bundlename";
	public static final String OSGISERVICE_BUNDLE_FILE = LOG_TAG + ".bundlefile";
	public static final String OSGISERVICE_BIND = LOG_TAG + ".bind";
	
	private String defaultAppDir = null;
	private String defaultFelixDir = null;
	
	private HostActivator hostActivator = null;
	private Felix felix = null;
	
	private Properties properties;
	
	private final IBinder osgiBinder = new OsgiBinder();
	
	private LinkedList<String> log;
	
	private HashMap<String, ServiceTracker<?, ?>> serviceTrackerMap = new HashMap<>();
	
	
	
	@Override
	public IBinder onBind(Intent intent) {
		Log.d(LOG_TAG, "START onBind(Intent)");
		
		IBinder binder;
		
		String action = intent.getAction();
		Bundle bundle = intent.getExtras();
		
		if((action != null) || (bundle != null)) {
			Log.d(LOG_TAG, "proxy bind for osgi service");
			
			String bundleFile = bundle.getString(OSGISERVICE_BUNDLE_FILE);
			String bundleName = bundle.getString(OSGISERVICE_BUNDLE_NAME);
			
			try {				
				Object serviceObject = getServiceObject(bundleName, bundleFile);
				
				if(serviceObject == null) {
					binder = null;
				} else {
					binder = new ProxyBinder(serviceObject);
				}
			} catch (InterruptedException | BundleException e) {
				binder = null;
			}
		} else {
			Log.d(LOG_TAG, "native start for android service");
			
			binder = osgiBinder;
		}
		
		Log.d(LOG_TAG, "END onBind(Intent)");
		
		return binder;
	}
	
	@Override
	public void onRebind(Intent intent) {
		Log.d(LOG_TAG, "START onRebind(Intent)");
		
		Log.d(LOG_TAG, "END onRebind(Intent)");
	}
	
	@Override
	public void onCreate() {
		Log.d(LOG_TAG, "START onCreate()");
		
		log = new LinkedList<>();
		
		Runnable r = new Runnable() {
			public void run() {
				Log.d(LOG_TAG, "START run()");
				
				Log.d(LOG_TAG, "VAR: " + OSGISERVICE_START);
				
				launchFelix();

				checkBundles();
				
				Log.d(LOG_TAG, "END run()");
			}
		};

		Thread t = new Thread(r);
		t.start();

		Log.d(LOG_TAG, "END onCreate()");
	}
	
	@Override
    public void onDestroy() {
    	Log.d(LOG_TAG, "START onDestroy()");
    	
    	Log.d(LOG_TAG, "END onDestroy()");
    }
	
	private void launchFelix() {
		Log.d(LOG_TAG, "START launchFelix()");

		try {
			defaultAppDir = File.createTempFile("felix-cache", ".tmp").getParent();
			Log.d(LOG_TAG, "default app dir is: " + defaultAppDir);

			File felixStore = new File(Constants.DIR_FELIX_NAME);
			Utils.preparePath(felixStore);
			defaultFelixDir = felixStore.getAbsolutePath();
			Log.d(LOG_TAG, "felix store path is: " + defaultFelixDir);
		} catch (IOException e) {
			Log.e(LOG_TAG, "unable to create cache directory");
			return;
		}
        
        setFelixProperties();
        
		try {
			Log.d(LOG_TAG, "instantiate felix");
			felix = new Felix(properties);
			Log.d(LOG_TAG, "initialize felix");
			felix.init();
			Log.d(LOG_TAG, "felix state after initialization: " + Utils.translateState(felix.getState()));
			Log.d(LOG_TAG, "starting felix");
			felix.start();
			Log.d(LOG_TAG, "felix state after start: " + Utils.translateState(felix.getState()));
		} catch (Throwable ex) {
			Log.e(LOG_TAG, "could not create felix framework: " + ex.getMessage());
		}

		Log.d(LOG_TAG, "END launchFelix()");
	}
	
	private void setFelixProperties() {
		Log.d(LOG_TAG, "START setProperties()");
		
//		properties = new Properties();
//		
//		/*
//		 *  Import properties from config.properties file stored as raw resource.
//		 */
//		try {
//			Resources res = this.getResources();
//			InputStream is = res.openRawResource(R.raw.config);
//			properties.load(is);
//			Log.d(LOG_TAG, "successfully loaded properties file");
//			Log.d(LOG_TAG, "properties loaded from file are:");
//			printProperties();
//		} catch (NotFoundException e) {
//			Log.d(LOG_TAG, "properties file not found");
//		} catch (IOException e) {
//			Log.d(LOG_TAG, "failed to load properties file");
//		}
		
		properties = new Properties();
		
		// set properties manually here.
		properties.put("felix.log.level", "4");
		properties.put("org.osgi.framework.storage.clean", "onFirstInit"); // clean cache on start
//		properties.put("org.osgi.framework.storage.clean", "none");        // keep cache on start
//		properties.put("osgi.shell.telnet", "on");                         // activate telnet console
		
//		properties.put("felix.cache.rootdir", defaultFelixDir);
		
		// use JVM "boot class loader" for loading the provided classes
		// supports wildcards
		properties.put("org.osgi.framework.bootdelegation", Constants.BOOT_DELEGATION_PACKAGES);
		
		// export the provided packages and provide them to other bundles
		properties.put("org.osgi.framework.system.packages.extra", Constants.ANDROID_FRAMEWORK_PACKAGES_ext);
		
		// use the provided folder for caching
		properties.put("org.osgi.framework.storage", defaultAppDir);
		
		// no longer needed as of Apache Felix Framework 2.0.0
		properties.put("felix.embedded.execution", "true");
		
		properties.put("org.osgi.service.http.port", "8080");
		
		properties.put("org.osgi.framework.startlevel.beginning", "5");
		
		List<BundleActivator> activatorList = new ArrayList<BundleActivator>();
		
		hostActivator = new HostActivator(this);
		
		Log.d(LOG_TAG, "current hostActivator is : " + hostActivator);
        activatorList.add(hostActivator);
        properties.put(FelixConstants.SYSTEMBUNDLE_ACTIVATORS_PROP, activatorList);        
        
        Log.d(LOG_TAG, "properties actually used are : ");
		printProperties();
		
		Log.d(LOG_TAG, "END setProperties()");
	}
	
	/**
	 * This method prints the current content of the felix properties.
	 */
	private void printProperties() {
		Log.d(LOG_TAG, "START printProperties()");
		
		for(String propertyKey:properties.stringPropertyNames()) {
			String propertyValue = properties.getProperty(propertyKey);
			
			Log.d(LOG_TAG, "property \"" + propertyKey + "\" --> \"" + propertyValue + "\"");
		}
		
		Log.d(LOG_TAG, "END printProperties()");
	}
	
	/**
	 * This method prints a list of all bundles and their parameters known to Felix.
	 */
	private void checkBundles() {
		Log.d(LOG_TAG, "START listKnownBundles()");
		
		boolean allBundlesActive = true;
		
		org.osgi.framework.Bundle[] bundlesLoaded;
		
		try{
			Log.d(LOG_TAG, "============================================================");
			
            bundlesLoaded = felix.getBundleContext().getBundles();
            Log.d(LOG_TAG, "Found " + bundlesLoaded.length + " bundles");
            
            for(org.osgi.framework.Bundle b : bundlesLoaded) {
            	Log.d(LOG_TAG, "Bundle: " + b.getSymbolicName() + " [" + b.getBundleId() + "]");
            }
            
            Log.d(LOG_TAG, "============================================================");
            
            for(org.osgi.framework.Bundle bundle : bundlesLoaded) {
            	Log.d(LOG_TAG, "Bundle: " + bundle.getSymbolicName() + " [" + bundle.getBundleId() + "] at " + bundle.getLocation());
            	Log.d(LOG_TAG, "Bundle state is: " + Utils.translateState(bundle.getState()));
            	
            	if(bundle.getState() != org.osgi.framework.Bundle.ACTIVE) {
            		allBundlesActive = false;
            	}
            	
            	Object bundleServiceObject;
            	BundleContext bundleContext;
            	ServiceReference<?>[] srs = bundle.getRegisteredServices();
            	
            	if(srs == null) {
            		Log.d(LOG_TAG, "bundle does not provide any services");
            	} else {
            		for(ServiceReference<?> sr:srs) {
                		if(sr != null) {
            				bundleContext = bundle.getBundleContext();
            				bundleServiceObject = bundleContext.getService(sr);
            				Log.d(LOG_TAG, "service = " + bundleServiceObject);
            				if(bundleServiceObject != null) {
            					Log.d(LOG_TAG, "service is of type: " + bundleServiceObject.getClass());
            				}
        					
            			}
                	}
            	}
            	
            	Log.d(LOG_TAG, "============================================================");
            }
        } catch (Throwable ex) {
        	Log.e(LOG_TAG, "could not create framework: " + ex.getMessage());
        }
		
		/*
		 * Send information via broadcast especially if not all bundles have been loaded.
		 * If e.g. loading of logging bundle fails this may otherwise go unnoticed. 
		 */
		Intent intent = new Intent(OSGISERVICE_STATUS);
		if(allBundlesActive) {
			intent.putExtra(OSGISERVICE_STATUS_MESSAGE, "All OSGI services ACTIVE!");
		} else {
			intent.putExtra(OSGISERVICE_STATUS_MESSAGE, "Not all OSGI services ACTIVE!\nFunctionality of respective bundles, e.g. log output, may not be available");
		}
		
        sendBroadcast(intent);
		
		Log.d(LOG_TAG, "END listKnownBundles()");
	}
	
	@Override
	  public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(LOG_TAG, "START onStartCommand(Intent, int, int)");
		
		int result;
		
		String action = intent.getAction();
		Bundle bundle = intent.getExtras();
		
		if(action.equals(OSGISERVICE_START)) {
			Log.d(LOG_TAG, "proxy start for osgi service");
			
			String bundleFile = bundle.getString(OSGISERVICE_BUNDLE_FILE);
			String bundleName = bundle.getString(OSGISERVICE_BUNDLE_NAME);
			
			try {				
				Object serviceObject = getServiceObject(bundleName, bundleFile);
				
				if(serviceObject == null) {
					result = START_REDELIVER_INTENT;
				} else {
					result = START_STICKY_COMPATIBILITY; // TODO check for better options
				}
			} catch (InterruptedException | BundleException e) {
				// original intent is rescheduled so service may be started later
				result = START_REDELIVER_INTENT;
			}
		} else {
			Log.d(LOG_TAG, "native start for android service");
			
			result = super.onStartCommand(intent, flags, startId);
		}
		
		Log.d(LOG_TAG, "result is: " + result);
		Log.d(LOG_TAG, "END onStartCommand(Intent, int, int)");
		
		return result;
	}
	
	protected Object getServiceObject(String bundleName, String bundleFile) throws BundleException, InterruptedException {
		BundleContext bundleContext = felix.getBundleContext();
		
		org.osgi.framework.Bundle osgiBundle = bundleContext.installBundle("file:" + bundleFile);
		
		osgiBundle.start();
		
		ServiceTracker<?, ?> serviceTracker;
		
		if(serviceTrackerMap.containsKey(bundleName)) {
			Log.d(LOG_TAG, "service tracker already registered");
			serviceTracker = serviceTrackerMap.get(bundleName);
		} else {
			Log.d(LOG_TAG, "registering new service tracker");
			serviceTracker = new ServiceTracker<>(getBundleContext(), bundleName, null);
			serviceTracker.open();
			serviceTrackerMap.put(bundleName, serviceTracker);
		}
		
		Object serviceObject = serviceTracker.waitForService(10000);
		Log.d(LOG_TAG, "service object is: " + serviceObject);
		if(serviceObject != null) {
			Log.d(LOG_TAG, "service object type is: " + serviceObject.getClass().getSimpleName());
		}
		
		return serviceObject;
	}
	
	/**
	 * This method returns the Felix BundleContext.
	 * @return the Felix BundleContext
	 */
	public BundleContext getBundleContext() {
		return felix.getBundleContext();
	}
	
	public class OsgiBinder extends Binder {
		OsgiService getService() {
			Log.d(LOG_TAG, "OsgiBinder.getService()");
			return OsgiService.this;
		}
	}
	
	public class ProxyBinder extends Binder {
		private Object object;
		
		public ProxyBinder(Object binderObject) {
			object = binderObject;
		}
		
		Object getService() {
			Log.d(LOG_TAG, "ProxyBinder.getService()");
			return object;
		}
	}
	
	public synchronized List<String> fetchLog() {
		if(log.size() > 0) {
			List<String> list = log;
			log = new LinkedList<>();
			return list;
		} else {
			return null;
		}
	}
	
	public synchronized void addToLog(String message) {
		log.add(message);
	}
	
}
