package de.persosim.android.app;

import org.osgi.framework.BundleContext;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;



/**
 * This service provides the interface to Android's NFC host card emulation (HCE) support added in Android 4.4, KitKat.
 * It is used as a bridge between card reader and PeroSim.
 *
 * <p>This service will be invoked for any terminals selecting AIDs
 * specified in src/main/res/xml/aid_list.xml.
 * @author slutters
 * 
 */
public class CardService extends HostApduService implements Iso7816, Constants {
	public static final String PACKAGE_NAME = "de.persosim.android.techdemo";
	
    private static final String LOG_TAG = CardService.class.getName();
    
    public static final String CARDSERVICE_COMMAND = LOG_TAG + ".command";
    public static final String CARDSERVICE_PERSONALIZATION = LOG_TAG + ".personalization";
    
    public static final String CARDSERVICE_COMMAND_STRING = CARDSERVICE_COMMAND + ".string";
    public static final String CARDSERVICE_PERSONALIZATION_PATH = CARDSERVICE_PERSONALIZATION + ".path";
    
    // object available when CardService.onCreate() has finished
    private OsgiService osgiServiceObject;
	private ServiceConnection serviceConnection;
	private BundleContext bundleContext;
	private PersoSimWrapper sim;
	private boolean isReset = true;
	
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(LOG_TAG, "START onReceive");
			
			String action = intent.getAction();
			
			if(action.equals(CardService.CARDSERVICE_COMMAND)) {
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					String cmd = bundle.getString(CARDSERVICE_COMMAND_STRING);
					
					executeCommand(cmd);
				}
			} else {
				if(action.equals(CardService.CARDSERVICE_PERSONALIZATION)) {
					Log.d(LOG_TAG, "received personalization");
					Bundle bundle = intent.getExtras();
					if (bundle != null) {
						String path = bundle.getString(CARDSERVICE_PERSONALIZATION_PATH);
						executeCommand("loadperso " + path);
					}
				} else {
					Log.d(LOG_TAG, "received unknown brodcast message");
				}
			}
			
			Log.d(LOG_TAG, "END onReceive");
		}
	};
	
	/**
     * This method is called on reception and prior to processing of any command APDU following a card reset.
     * It sends a management command APDU to the APDU processor indicating a card reset. 
     */
	private void onActivated() {
		Log.d(LOG_TAG, "START onActivated(int)");
		
		byte[] apdu = Utils.HexStringToByteArray("FFFF0000");
		sim.processCommand(apdu);
		
		Log.d(LOG_TAG, "END onActivated(int)");
	}
	
    @Override
	public void onDeactivated(int reason) {
		Log.d(LOG_TAG, "START onDeactivated(int)");

		switch (reason) {
		case DEACTIVATION_DESELECTED:
			Log.d(LOG_TAG, "Card reset due to deselect");
			break;
		case DEACTIVATION_LINK_LOSS:
			Log.d(LOG_TAG, "Card reset due to link loss");
			break;
		default:
			Log.d(LOG_TAG, "Card reset due to unknown reason (" + reason + ")");
			break;
		}
		
		Log.i(LOG_TAG, "Card has been reset");
		isReset = true;
		
		Log.d(LOG_TAG, "END onDeactivated(int)");
	}
    
    @Override
    public void onCreate() {
    	Log.d(LOG_TAG, "START onCreate()");
    	
    	registerReceiver(receiver, new IntentFilter(CARDSERVICE_COMMAND));
    	registerReceiver(receiver, new IntentFilter(CARDSERVICE_PERSONALIZATION));
    	
    	bindService();
    	
    	Log.d(LOG_TAG, "END onCreate()");
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
    	Log.d(LOG_TAG, "START processCommandApdu");
    	
    	Log.i(LOG_TAG, "received new command APDU: " + Utils.encode(commandApdu));
    	
    	sim = getSim();
    	if (sim == null) {
    		return new byte[]{0x6F,0x6F};
    	}
    	
    	/**
    	 *  Call method onActivated() analog to inherited onDeactivated(int).
    	 *  Variable {@link #sim} 
    	 */
    	if(isReset) {
    		Log.d(LOG_TAG, "card previously received reset instruction - perform reset now, then process received APDU");
    		onActivated();
    		isReset = false;
    	}
    	
    	byte[] responseApdu = Utils.toUnsignedByteArray(SW_9000_NO_ERROR);
    	
    	responseApdu = sim.processCommand(commandApdu);
    	
    	Log.i(LOG_TAG, "received new response APDU: " + Utils.encode(responseApdu));
    	
    	Log.d(LOG_TAG, "END processCommandApdu");
    	
    	return responseApdu;
    	
    }
    
    /**
     * This method binds, i.e. starts, the OSGI service.
     */
    private void bindService() {
    	setServiceConnection();
    	
    	Intent intent = new Intent(this, OsgiService.class);
    	Log.d(LOG_TAG, "intent is: " + intent);
    	Log.d(LOG_TAG, "connection is: " + serviceConnection);
    	
    	boolean isBound;
    	
    	isBound = bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    	Log.d(LOG_TAG, "service is bound: " + isBound);
        
        Log.d(LOG_TAG, "bound service is: " + osgiServiceObject);
    }
    
    /**
     * This method returns the {@link PersoSimWrapper} object if available or retrievable.
     * @return the {@link PersoSimWrapper} object if available or retrievable
     */
    private PersoSimWrapper getSim() {
    	if(sim == null) {
    		bundleContext = osgiServiceObject.getBundleContext();
    		
			/**
			 * Simulator is set here as variable {@link #bundleContext} depends
			 * on {@link #osgiServiceObject} which may not have been initialized
			 * earlier.
			 */
    		try {
    			Log.d(LOG_TAG, "establishing connection to simulator");
    			sim = new PersoSimWrapper(bundleContext);
    			Log.d(LOG_TAG, "connection to simulator established");
    		} catch(Exception e) {
    			Log.e(LOG_TAG, "unable to connect to simulator");
    		}
    	}
    	return sim;
	}
    
	/**
	 * This method sets the service connection required to bind the OSGI service.
	 */
    private void setServiceConnection() {
    	serviceConnection = new ServiceConnection() {	
    		@Override
    		public void onServiceConnected(ComponentName className, IBinder binder) {
    			Log.d(LOG_TAG, "START onServiceConnected");
    			OsgiService.OsgiBinder osgiBinder = (OsgiService.OsgiBinder) binder;
    			osgiServiceObject = osgiBinder.getService();
    			Log.d(LOG_TAG, "OsgiService is: " + osgiServiceObject);
    			if(osgiServiceObject != null) {
    				Log.d(LOG_TAG, "service is of type: " + osgiServiceObject.getClass().getName());
    			}
    			Log.d(LOG_TAG, "END onServiceConnected");
    		}
    		
    		@Override
    		public void onServiceDisconnected(ComponentName className) {
    			Log.d(LOG_TAG, "START onServiceDisConnected");
    			osgiServiceObject = null;
    			Log.d(LOG_TAG, "END onServiceDisConnected");
    		}
    	};
    }
    
    /**
     * This method has user commands executed by the simulator.
     * @param cmdString the user command to be executed
     */
    public void executeCommand(String cmdString) {
    	sim = getSim();
    	if (sim == null) {
    		Log.e(LOG_TAG, "Unable to handle command - no simulator available");	
    		return;
    	}
    	
    	Log.i(LOG_TAG, "executing user command: " + cmdString);
    	sim.executeUserCommands(cmdString);
    }
	
}
