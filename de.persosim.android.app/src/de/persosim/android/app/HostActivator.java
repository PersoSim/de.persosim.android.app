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
	
	private ServiceRegistration<?> serviceRegistrationBundle;
	private OsgiService osgiService;
	
	public String fileNameFelixLog      = "org.apache.felix.log-1.0.1.jar";
	public String fileNameSimulator     = "de.persosim.simulator_0.5.0.SNAPSHOT.jar";
	public String fileNameCryptProv     = "org.globaltester.cryptoprovider.jar";
	public String fileNameSc            = "org.globaltester.cryptoprovider.sc.jar";
	public String fileNameAndroidLogger = "de.persosim.android.logging.consolelogger.jar";
	public String fileNameLogging       = "org.globaltester.logging.jar";
	
	public String fileNameProfile01 = "Profile01.xml";
	public String fielNameProfile02 = "Profile02.xml";
	public String fileNameProfile03 = "Profile03.xml";
	public String fileNameProfile04 = "Profile04.xml";
	public String fileNameProfile05 = "Profile05.xml";
	public String fileNameProfile06 = "Profile06.xml";
	public String fileNameProfile07 = "Profile07.xml";
	public String fileNameProfile08 = "Profile08.xml";
	public String fileNameProfile09 = "Profile09.xml";
	public String fileNameProfile10 = "Profile10.xml";
	
	public String[] startOrder = {fileNameLogging, fileNameAndroidLogger, fileNameCryptProv, fileNameSc, fileNameSimulator};
	
	List<org.osgi.framework.Bundle> installedBundlesUnordered;
	List<org.osgi.framework.Bundle> installedBundlesOrderedNonStart;
	List<org.osgi.framework.Bundle> installedBundlesOrderedStart;
	List<org.osgi.framework.Bundle> installedBundlesNoStart;
	
	
	
	public HostActivator(OsgiService osgiService) {
		this.osgiService = osgiService;
	}
	
	@Override
	public void start(BundleContext context) {
		Log.d(LOG_TAG, "START start(BundleContext)");
		
		bundleContext = context;
		
		Log.d(LOG_TAG, "START check dumping");
		dumpBundlesFromApk();
		Log.d(LOG_TAG, "END check dumping");
		
		List<String> bundleListOrderedNonStart = Utils.listFileNamesForFolder(Constants.DIR_BUNDLE_ORDERED_START_NAME, ".jar", true);
		
		List<String> bundleListOrderedStart = new ArrayList<>();
		String bundleFileName;
		for(String bundle : startOrder) {
			bundleFileName = Constants.DIR_BUNDLE_ORDERED_START_NAME + "/" + bundle;
			bundleListOrderedStart.add(bundleFileName);
		}
		
		Log.d(LOG_TAG, "install ordered bundles start");
		installedBundlesOrderedStart    = installBundlesFromDisk(bundleListOrderedStart);
		Log.d(LOG_TAG, "install ordered bundles non start");
		installedBundlesOrderedNonStart = installBundlesFromDisk(bundleListOrderedNonStart);
		
		Log.d(LOG_TAG, "install unordered bundles");
		List<String>  bundleList = Utils.listFileNamesForFolder(Constants.DIR_BUNDLE_UNORDERED_START_NAME, ".jar", true);
		installedBundlesUnordered = installBundlesFromDisk(bundleList);
		
		Log.d(LOG_TAG, "start ordered bundles");
		startBundles(installedBundlesOrderedStart);
		Log.d(LOG_TAG, "start unordered bundles");
		startBundles(installedBundlesUnordered);
		
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
	 * This method installs all bundles contained in the provided list.
	 * @param bundleList the list of bundle files to be installed
	 */
	private List<org.osgi.framework.Bundle> installBundlesFromDisk(List<String> bundleList) {
		Log.d(LOG_TAG, "START installBundlesFromDisk(List<String>)");
        
		List<org.osgi.framework.Bundle> bundlesInstalled = new ArrayList<>();
        
        String bundlePath;
        
        try {
        	for(String bundleName : bundleList) {
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
        
        Log.d(LOG_TAG, "END installBundlesFromDisk(List<String>)");
        
        return bundlesInstalled;
	}
	
	/**
	 * This method exports bundles stored within the raw resources to the app's bundle directory.
	 */
	private void dumpBundlesFromApk() {
		Log.d(LOG_TAG, "START dumpBundlesFromApk()");
		
//		String pathBundles = Constants.DIR_BUNDLE_NAME;
		String pathPerso = Constants.DIR_PERSO_NAME;
		
//		File file = new File(pathBundles);
//		Utils.preparePath(file);
		
		File file = new File(pathPerso);
		Utils.preparePath(file);
		
		Log.d(LOG_TAG, "dumping bundles to disk");
		Utils.writeRawResourceToFile(osgiService, R.raw.felixlog,         Constants.DIR_BUNDLE_UNORDERED_START_NAME, fileNameFelixLog);
		
		Utils.writeRawResourceToFile(osgiService, R.raw.cryptoprovider,   Constants.DIR_BUNDLE_ORDERED_START_NAME, fileNameCryptProv);
		Utils.writeRawResourceToFile(osgiService, R.raw.cryptoprovidersc, Constants.DIR_BUNDLE_ORDERED_START_NAME, fileNameSc);
		
		Utils.writeRawResourceToFile(osgiService, R.raw.simulator,        Constants.DIR_BUNDLE_ORDERED_START_NAME, fileNameSimulator);
		Utils.writeRawResourceToFile(osgiService, R.raw.androidlogger,    Constants.DIR_BUNDLE_ORDERED_START_NAME, fileNameAndroidLogger);
		Utils.writeRawResourceToFile(osgiService, R.raw.logging,          Constants.DIR_BUNDLE_ORDERED_START_NAME, fileNameLogging);
		
		Log.d(LOG_TAG, "dumping personalizations to: " + pathPerso);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile01, pathPerso, fileNameProfile01);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile02, pathPerso, fielNameProfile02);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile03, pathPerso, fileNameProfile03);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile04, pathPerso, fileNameProfile04);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile05, pathPerso, fileNameProfile05);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile06, pathPerso, fileNameProfile06);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile07, pathPerso, fileNameProfile07);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile08, pathPerso, fileNameProfile08);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile09, pathPerso, fileNameProfile09);
		Utils.writeRawResourceToFile(osgiService, R.raw.profile10, pathPerso, fileNameProfile10);
		
		String pgt = "ProfileGT.xml";
		Utils.writeRawResourceToFile(osgiService, R.raw.profilegt, pathPerso, pgt);

		Log.d(LOG_TAG, "END dumpBundlesFromApk()");
	}
	
	/**
	 * This method starts all bundles that have been previously installed.
	 */
	public void startBundles(List<org.osgi.framework.Bundle> bundlesInstalled) {
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
