package de.persosim.android.app;

import de.persosim.android.app.R;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;



/**
 * This class provides the functionality provided by the "Configuration" dialog.
 * @author slutters
 *
 */
public class DialogConfig extends DialogFragment {
	
	public static final String KEY_IS_APDU_CATCH_ALL_ACTIVE = "de.persosim.android.isApduCatchAllActive";
    public static final String KEY_MAGIC_AID = "de.persosim.android.magicAid";
	
	private Button buttonOk, buttonCancel;
	private static Context activityContext;
	

	
	public interface SelectDialogListener {
        void onFinishEditDialog(String inputText);
    }
    
	public DialogConfig(Context newActivityContext) {
        activityContext = newActivityContext;
       
    }
	private Spinner spinner;
	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container);
    
        spinner = (Spinner) view.findViewById(R.id.planets_spinner);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
				activityContext, R.array.planets_array,
				android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(adapter);
        
        getDialog().setTitle("Configuration");
        
        buttonOk = (Button) view.findViewById(R.id.buttonConfigOk);
        buttonOk.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	// do something
            	
                getDialog().dismiss();
            }
        });
        
        buttonCancel = (Button) view.findViewById(R.id.buttonConfigCancel);
        buttonCancel.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	int logLevel = spinner.getLastVisiblePosition() + 1;
            	
                getDialog().dismiss();
            }
        });

        return view;
    }
	
}
