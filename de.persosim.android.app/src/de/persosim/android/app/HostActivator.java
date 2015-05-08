package de.persosim.android.app;

import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceRegistration;

import de.persosim.android.app.R;
import android.content.Context;
import android.util.Log;



public class HostActivator implements BundleActivator {
	
	public static final String LOG_TAG = HostActivator.class.getName();
	
	private BundleContext bundleContext = null;
	private List<org.osgi.framework.Bundle> bundlesInstalled;
	private ServiceRegistration<?> serviceRegistrationBundle;
	private OsgiService osgiService;
	
	
	
	public HostActivator(OsgiService osgiService) {
		this.osgiService = osgiService;
	}
	
	@Override
	public void start(BundleContext context) {
		Log.d(LOG_TAG, "START start(BundleContext)");
		
		bundleContext = context;
		
		dumpBundlesFromApk();
		installBundlesFromDisk();
		startBundles();
		registerServices();
		
		Log.d(LOG_TAG, "END start(BundleContext)");
	}
	
	@Override
	public void stop(BundleContext context) {
		Log.d(LOG_TAG, "START stop(BundleContext)");
		
		bundleContext = null;
		
		Log.d(LOG_TAG, "END stop(BundleContext)");
	}
	
	/**
	 * This method installs all bundles found within the app's bundle directory or any of its sub-directories.
	 */
	private void installBundlesFromDisk() {
		Log.d(LOG_TAG, "START installBundlesFromDisk()");
        
        bundlesInstalled = new ArrayList<>();
        
        String bundlePath, bundleName;
        List<String> bundleList = Utils.listFileNamesForFolder(Constants.DIR_BUNDLE_NAME, ".jar", true);
        
        try {
        	for(String bundleElem : bundleList) {
        		bundleName = bundleElem;
        		bundlePath = "file:" + bundleName;
        		
        		Log.d(LOG_TAG, "about to install bundle at path: " + bundlePath);
        		Bundle currentBundle = bundleContext.installBundle(bundlePath);
            	
            	if(currentBundle != null) {
            		bundlesInstalled.add(currentBundle);
            		Log.d(LOG_TAG, "successfully installed bundle: " + currentBundle.getSymbolicName() + " (" + bundleName + " - " + currentBundle.getClass() + ")" + ", bundle version is: " + currentBundle.getVersion().toString());
            		Log.d(LOG_TAG, "bundle state is: " + Utils.translateState(currentBundle.getState()));
            	} else {
            		Log.w(LOG_TAG, "ERROR: skipped installation of bundle");
            	}
        	}
			
        	Log.d(LOG_TAG, "successfully installed all bundles");
		} catch (BundleException e) {
			Log.w(LOG_TAG, "encountered BundleException when installing bundle: " + e.getMessage());
		} catch (Exception e) {
			Log.w(LOG_TAG, "encountered general Exception when installing bundle: " + e.getMessage());
		}
        
        Log.d(LOG_TAG, "END installBundlesFromDisk()");
	}
	
	/**
	 * This method exports bundles stored within the raw resources to the app's bundle directory.
	 */
	private void dumpBundlesFromApk() {
		Log.d(LOG_TAG, "START dumpBundlesFromApk");
		
		String pathBundles = Constants.DIR_BUNDLE_NAME;
		String pathPerso = Constants.DIR_PERSO_NAME;
		
		File file = new File(pathBundles);
		Utils.preparePath(file);
		
		file = new File(pathPerso);
		Utils.preparePath(file);
		
		String fileNameFelixLog      = "org.apache.felix.log-1.0.1.jar";
		String fileNameSimulator     = "de.persosim.simulator_0.5.0.SNAPSHOT.jar";
		String fileNameCryptProv     = "org.globaltester.cryptoprovider.jar";
		String fileNameSc            = "org.globaltester.cryptoprovider.sc.jar";
		String fileNameAndroidLogger = "de.persosim.android.logging.consolelogger.jar";
		String fileNameLogging       = "org.globaltester.logging.jar";
		
		Log.d(LOG_TAG, "dumping bundles to: " + pathBundles);
		Utils.writeRawResourceToFile(osgiService, R.raw.felixlog, pathBundles, fileNameFelixLog);
		
		Utils.writeRawResourceToFile(osgiService, R.raw.cryptoprovider, pathBundles, fileNameCryptProv);
		Utils.writeRawResourceToFile(osgiService, R.raw.cryptoprovidersc, pathBundles, fileNameSc);
		
		Utils.writeRawResourceToFile(osgiService, R.raw.simulator, pathBundles, fileNameSimulator);
		Utils.writeRawResourceToFile(osgiService, R.raw.androidlogger, pathBundles, fileNameAndroidLogger);
		Utils.writeRawResourceToFile(osgiService, R.raw.logging, pathBundles, fileNameLogging);
		
		String p01 = "Profile01.xml";
		String p02 = "Profile02.xml";
		String p03 = "Profile03.xml";
		String p04 = "Profile04.xml";
		String p05 = "Profile05.xml";
		String p06 = "Profile06.xml";
		String p07 = "Profile07.xml";
		String p08 = "Profile08.xml";
		String p09 = "Profile09.xml";
		String p10 = "Profile10.xml";
		
		Log.d(LOG_TAG, "dumping personalizations to: " + pathPerso);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile01, pathPerso, p01);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile02, pathPerso, p02);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile03, pathPerso, p03);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile04, pathPerso, p04);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile05, pathPerso, p05);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile06, pathPerso, p06);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile07, pathPerso, p07);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile08, pathPerso, p08);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile09, pathPerso, p09);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile10, pathPerso, p10);
		
		String pgt = "ProfileGT.xml";
		Utils.writeRawResourceToFile(osgiService, R.raw.profilegt, pathPerso, pgt);

		Log.d(LOG_TAG, "END dumpBundlesFromApk");
	}
	
	/**
	 * This method starts all bundles that have been previously installed.
	 */
	public void startBundles() {
		Log.d(LOG_TAG, "START startBundles");

		for (org.osgi.framework.Bundle bundle : bundlesInstalled) {
			try {
				Log.d(LOG_TAG, "Bundle " + bundle.getSymbolicName() + " with id " + bundle.getBundleId() + " state pre start: " + Utils.translateState(bundle.getState()));
				
				bundle.start();
				
				Log.d(LOG_TAG, "Bundle " + bundle.getSymbolicName() + " with id " + bundle.getBundleId() + " state post start: " + Utils.translateState(bundle.getState()));

			} catch (BundleException e) {
				Log.w("Felix", "Encountered exception when starting bundle: " + e.getMessage(), e);
			}
		}

		Log.d(LOG_TAG, "END startBundles");
	}
	
	public void registerServices() {
		Log.d(LOG_TAG, "START registerServices");

		Log.d(LOG_TAG, "register Bundle for " + Context.class.getName());
		serviceRegistrationBundle = bundleContext.registerService(Context.class, osgiService, new Hashtable<String, String>());
		Log.d(LOG_TAG, "service registration for Bundle " + Context.class.getName() + " is: " + serviceRegistrationBundle);
		
		Log.d(LOG_TAG, "END registerServices");
	}

}
