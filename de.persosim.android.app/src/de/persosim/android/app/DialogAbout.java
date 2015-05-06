package de.persosim.android.app;

import de.persosim.android.app.R;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;



/**
 * This class provides the functionality provided by the "About" dialog.
 * @author slutters
 *
 */
public class DialogAbout extends DialogFragment {
	
	private TextView textViewLarge;
	private Button button;
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container);
        getDialog().setTitle("About");
        textViewLarge = (TextView) view.findViewById(R.id.aboutDialogTextViewLarge);
        textViewLarge.append(" v " + this.getPackageVersion());
        
        button = (Button) view.findViewById(R.id.aboutDialogButton);
        
        button.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                getDialog().dismiss();
            }
        });
        
        return view;
    }
    
    /**
	 * This method returns the version of this package as defined in the Manifest file.
	 * @return the version of this package as defined in the Manifest file
	 */
	public String getPackageVersion() {
		FragmentActivity activity = this.getActivity();
		PackageManager packageManager = activity.getPackageManager();
		String packageName = activity.getPackageName();
		PackageInfo pInfo;
		String packageVersion;
		
		try {
			pInfo = packageManager.getPackageInfo(packageName, 0);
			packageVersion = pInfo.versionName;
		} catch (NameNotFoundException e) {
			packageVersion = "unknown";
		}
		
		return packageVersion;
    }
	
}
