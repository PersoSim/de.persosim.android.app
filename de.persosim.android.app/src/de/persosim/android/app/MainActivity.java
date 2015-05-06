package de.persosim.android.app;

import java.util.List;

import de.persosim.android.app.DialogSelect.SelectDialogListener;
import de.persosim.android.app.R;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.KeyEvent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;



/**
 * This class provides the main GUI for the PersoSim app.
 * @author slutters
 * 
 */
public class MainActivity extends FragmentActivity implements Constants, SelectDialogListener {
	
	public static final String LOG_TAG = MainActivity.class.getName();
	
	public static final String KEY_IS_APDU_CATCH_ALL_ACTIVE = "de.persosim.android.isApduCatchAllActive";
    public static final String KEY_MAGIC_AID = "de.persosim.android.magicAid";
	
	private TextView textOut;
	private EditText textIn;
	
	private static final int lineLimit = 500;
	
	// object available when CardService.onCreate() has finished
    private OsgiService osgiServiceObject;
    private ServiceConnection serviceConnection;
    
	private BroadcastReceiver receiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			
			if(action.equals(OsgiService.OSGISERVICE_STATUS)) {
				Bundle bundle = intent.getExtras();
				if (bundle != null) {
					String status;
					
					status = bundle.getString(OsgiService.OSGISERVICE_STATUS_MESSAGE);
					
					Log.d(LOG_TAG, "MainActivity received card service status: " + status);
					textOut.append("\nOSGI service announces:\n" + status);
				}
			} else{
				
			}
			
		}
	};
	
	protected void addToLog(String message) {
		Log.d(LOG_TAG, "log: " + message);
		if (textOut != null) {
			textOut.append("\n" + message);
		}
	}
	
	private void startLogging() {
		
		new Thread(new Runnable() {
	        public void run() {
	        	while(osgiServiceObject == null) {
	        		Log.d(LOG_TAG, "waiting for OSGI service to start logging");
	        		try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						Log.d(LOG_TAG, "sleep interrupted");
					}
	        	}
	        	
	        	Log.d(LOG_TAG, "OSGI service present is: " + osgiServiceObject);
	        	
	        	while(true) {
		            textOut.post(new Runnable() {
		                public void run() {
		                	List<String> list = osgiServiceObject.fetchLog();
		                	if((list != null) && (list.size() > 0)) {
		                		Log.d(LOG_TAG, "about to log " + list.size() + " messages to GUI");
		                		for(String message : list) {
			                    	addToLog(message);
			                    }
		                	}
		                }
		            });
		            
		            try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						Log.d(LOG_TAG, "sleep interrupted");
					}
	        	}
	        }
	    }).start();
		
	}
	
	protected void resetLog() {
		Log.d(LOG_TAG, "START resetLog()");
		
		textOut.setText(R.string.main_out_text);
		
		Log.d(LOG_TAG, "END resetLog()");
	}
	
	protected void executeCommand(String cmdString) {
		Log.d(LOG_TAG, "START executeCommand(Editable)");
		
		addToLog(cmdString);
		textIn.setText("");
		Log.d(LOG_TAG, "received manual command: " + cmdString.toString());
		
		Intent intent = new Intent(CardService.CARDSERVICE_COMMAND);

		intent.putExtra(CardService.CARDSERVICE_COMMAND_STRING, cmdString.toString());
		sendBroadcast(intent);
		
		Log.d(LOG_TAG, "END executeCommand(Editable)");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.d(LOG_TAG, "START onCreate(Bundle)");
		
		setContentView(R.layout.activity_main);
		
		registerReceiver(receiver, new IntentFilter(OsgiService.OSGISERVICE_STATUS));
		
		textIn = (EditText) findViewById(R.id.textIn);
		
		textIn.setOnEditorActionListener(new OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                	Log.d(LOG_TAG, "received manual command via send action");
                	executeCommand(textIn.getText().toString());
        			
                    handled = true;
                }
                return handled;
            }
        });
		
		final ScrollView scrollView = (ScrollView) findViewById(R.id.scrollView);
		
		textOut = (TextView) findViewById(R.id.textOut);
		
		textOut.addTextChangedListener(new TextWatcher() {

			@Override
			public void afterTextChanged(Editable arg0) {
				scrollView.fullScroll(ScrollView.FOCUS_DOWN);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// nothing to do here but method must be implemented
			}

			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
				// nothing to do here but method must be implemented
			}
		});
		
		textOut.append("\nWaiting for OSGI services to become ACTIVE");
		
		TextWatcher tw = new TextWatcher() {
			
			@Override
            public void afterTextChanged(Editable s) {
				textOut.removeTextChangedListener(this);

				int noOfLines = textOut.getLineCount();
				if(noOfLines > lineLimit) {
					textOut.setText("");
				}

				textOut.addTextChangedListener(this);
            }
			
			@Override
            public void beforeTextChanged(CharSequence s,
                    int start, int count, int after) {
				// do nothing
            }
			
			@Override
            public void onTextChanged(CharSequence s, int start,
                    int before, int count) {
				// do nothing
            }
        };
        
        textOut.addTextChangedListener(tw);
        
        textOut.setMovementMethod(new ScrollingMovementMethod());
		
		Log.d(LOG_TAG, "MainActivity about to start CardService");
		Intent intent = new Intent(this, CardService.class);
		startService(intent);
		
		bindService();
		
		startLogging();
		
		Log.d(LOG_TAG, "END onCreate(Bundle)");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.d(LOG_TAG, "START onResume()");
		
		registerReceiver(receiver, new IntentFilter(OsgiService.OSGISERVICE_STATUS));
		
		Log.d(LOG_TAG, "END onResume()");
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		Log.d(LOG_TAG, "START onPause()");
		
		unregisterReceiver(receiver);
		
		Log.d(LOG_TAG, "END onPause()");
	}
	
	/**
	 * This method is executed whenever the "ENTER" button of the main view is pressed.
	 * @param view the current View
	 */
	public void onButtonClick(View view) {
		Log.d(LOG_TAG, "START onButtonClick(View)");
		
		switch (view.getId()) {
		case R.id.button:
			Log.d(LOG_TAG, "received manual command via enter button");
			executeCommand(textIn.getText().toString());
			break;
		default:
			Log.d(LOG_TAG, "encountered unhandled button event");
			break;
		}
		
		Log.d(LOG_TAG, "END onButtonClick(View)");
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(LOG_TAG, "START onCreateOptionsMenu(Menu)");
		
		MenuInflater inflater = getMenuInflater();

		// Inflate the menu specified in the xml file at the path provided as
		// argument to the command.
		// This adds the defined menu items to the action bar if it is present.
		inflater.inflate(R.menu.main, menu);
		
		Log.d(LOG_TAG, "END onCreateOptionsMenu(Menu)");
		
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(LOG_TAG, "START onOptionsItemSelected(MenuItem)");
		
		switch (item.getItemId()) {
		case R.id.menu_item_clear_id:
			resetLog();
			break;
		case R.id.menu_item_personalization_id:
			showSelectDialog();
			break;
//		case R.id.menu_item_config_id:
//			showConfigDialog();
//			break;
		case R.id.menu_item_about_id:
			showAboutDialog();
			break;
		default:
			break;
		}
		
		Log.d(LOG_TAG, "END onOptionsItemSelected(MenuItem)");
		
		return true;
	}
	
	private void showAboutDialog() {
		Log.d(LOG_TAG, "START showAboutDialog()");
		
		FragmentManager fm = getSupportFragmentManager();
        DialogAbout aboutDialog = new DialogAbout();
        aboutDialog.show(fm, "fragment_about");
        
        Log.d(LOG_TAG, "END showAboutDialog()");
    }
	
	private void showSelectDialog() {
		Log.d(LOG_TAG, "START showSelectDialog()");
		
		FragmentManager fm = getSupportFragmentManager();
        DialogSelect selectDialog = new DialogSelect(this);
        selectDialog.show(fm, "fragment_select");
        
        Log.d(LOG_TAG, "END showSelectDialog()");
    }
	
//	private void showConfigDialog() {
//		Log.d(LOG_TAG, "START showConfigDialog()");
//		
//		FragmentManager fm = getSupportFragmentManager();
//        DialogConfig configDialog = new DialogConfig();
//        configDialog.show(fm, "fragment_config");
//        
//        Log.d(LOG_TAG, "END showConfigDialog()");
//    }

	@Override
    public void onFinishEditDialog(String inputText) {
        // do nothing, method must be implemented as inherited by SelectDialogListener
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
	
}
