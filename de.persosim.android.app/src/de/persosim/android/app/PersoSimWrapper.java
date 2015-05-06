package de.persosim.android.app;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import android.util.Log;

public class PersoSimWrapper {

	private static final String DE_PERSOSIM_SIMULATOR_COMMANDPARSER          = "de.persosim.simulator.CommandParser";
	private static final String DE_PERSOSIM_SIMULATOR_SIMULATOR              = "de.persosim.simulator.Simulator";
	private static final String DE_PERSOSIM_SIMULATOR_PERSO_PERSONALIZATION  = "de.persosim.simulator.perso.Personalization";
	private static final String DE_PERSOSIM_SIMULATOR_PERSO_DEFAULT_PERSO_GT = "de.persosim.simulator.perso.DefaultPersoGt";

	private static final String CMD_PROCESS_COMMAND       = "processCommand";
	private static final String CMD_LOAD_PERSONALIZATION  = "loadPersonalization";

	public static final String LOG_TAG = PersoSimWrapper.class.getName();
	
	private BundleContext bundleContext;
	
	private Object serviceObject;
	
	private Method methodLoadPersonalization;
	private Method methodProcessCommand;
	
	
	
	public PersoSimWrapper(BundleContext bundleContext) {
		Log.d(LOG_TAG, "START PersoSimWrapper(BundleContext)");
		
		this.bundleContext = bundleContext;
		
		ServiceTracker<?, ?> serviceTracker = new ServiceTracker<>(this.bundleContext, DE_PERSOSIM_SIMULATOR_SIMULATOR, null);
		serviceTracker.open();
		
		try {
			serviceObject = serviceTracker.waitForService(10000);
			Log.d(LOG_TAG, "service object is: " + serviceObject);
			
			// use correct class loader for loading method parameter class
			final ClassLoader serviceObjectClassLoader = serviceObject.getClass().getClassLoader();
			
			methodProcessCommand = serviceObject.getClass().getMethod(CMD_PROCESS_COMMAND, byte[].class);
			Log.d(LOG_TAG, "loaded method " + CMD_PROCESS_COMMAND);
			
			try {
				Class<?> classPersonalization = serviceObjectClassLoader.loadClass(DE_PERSOSIM_SIMULATOR_PERSO_PERSONALIZATION);
				methodLoadPersonalization = serviceObject.getClass().getMethod(CMD_LOAD_PERSONALIZATION, classPersonalization);
				Log.d(LOG_TAG, "loaded method " + CMD_LOAD_PERSONALIZATION);
				
				ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(serviceObjectClassLoader);
				
				// new thread required as (re-)creation of personalization
				// object leads to instantiation of several new objects which
				// need to be loaded with the same class loader.
				Thread t = new Thread(new Runnable() {

					@Override
					public void run() {
						try {
							
							Class<?> defaultPersoClazz = serviceObjectClassLoader.loadClass(DE_PERSOSIM_SIMULATOR_PERSO_DEFAULT_PERSO_GT);
						
							Constructor<?> ctor = defaultPersoClazz.getDeclaredConstructor();
						    ctor.setAccessible(true);
						    Object persoInstance = ctor.newInstance();
						    
					    	methodLoadPersonalization.invoke(serviceObject, persoInstance);
						} catch (NoSuchMethodException | ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							Log.d(LOG_TAG, "failed to load default personalization");
						}
					}
					
				});

				Thread.currentThread().setContextClassLoader(origClassLoader);
				
				t.start();
				t.join();
			} catch (ClassNotFoundException e) {
				Log.d(LOG_TAG, "failed to load default personalization");
			}
		} catch (NoSuchMethodException | IllegalArgumentException | InterruptedException e) {
			Log.d(LOG_TAG, "failed to retrieve PersoSim service");
		}
		
		Log.d(LOG_TAG, "END PersoSimWrapper(BundleContext)");
	}
	
	/**
	 * This method has command APDUs processed by the simulator.
	 * @param commandApdu the command APDU to be processed
	 * @return the resulting response APDU
	 */
	public byte[] processCommand(byte[] commandApdu) {
		Log.d(LOG_TAG, "START processCommand(byte[])");
		
		byte[] rApdu = Utils.HexStringToByteArray("6FFF");
		
		try {
			Object resultObject = methodProcessCommand.invoke(serviceObject, commandApdu);
			Log.d(LOG_TAG, "now casting response");
			rApdu = (byte[]) resultObject;
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			Log.e(LOG_TAG, "failed processing command APDU " + Utils.encode(commandApdu));
		}
		
		Log.d(LOG_TAG, "END processCommand(byte[])");
		
		return rApdu;
	}
	
	public void executeUserCommands(final String cmdString) {
		ClassLoader origClassLoader = Thread.currentThread().getContextClassLoader();
		Thread.currentThread().setContextClassLoader(serviceObject.getClass().getClassLoader());
		
		Thread t = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		
					Class<?> commandParserClazz = classLoader.loadClass(DE_PERSOSIM_SIMULATOR_COMMANDPARSER);
					Class<?> simulatorClazz = classLoader.loadClass(DE_PERSOSIM_SIMULATOR_SIMULATOR);
						
					Method m = commandParserClazz.getDeclaredMethod("executeUserCommands", simulatorClazz, String.class);
					m = commandParserClazz.getMethod("executeUserCommands", simulatorClazz, String.class);
					m.setAccessible(true);
					m.invoke(null, serviceObject, cmdString);
						
				} catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
				
		});

		Thread.currentThread().setContextClassLoader(origClassLoader);
		
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

}
