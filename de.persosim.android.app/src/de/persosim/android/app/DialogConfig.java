package de.persosim.android.app;

import de.persosim.android.app.R;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;



/**
 * This class provides the functionality provided by the "Configuration" dialog.
 * @author slutters
 *
 */
public class DialogConfig extends DialogFragment {
	
	public static final String KEY_IS_APDU_CATCH_ALL_ACTIVE = "de.persosim.android.isApduCatchAllActive";
    public static final String KEY_MAGIC_AID = "de.persosim.android.magicAid";
	
	private Button buttonOk, buttonCancel;
	
	public interface SelectDialogListener {
        void onFinishEditDialog(String inputText);
    }
    
	
	
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_config, container);
        
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
                getDialog().dismiss();
            }
        });

        return view;
    }
	
}
