package vpn.openconnect.ui;

import android.app.Activity;
import android.graphics.Color;
import android.widget.TextView;

/**
 * For logging the error messages and status messages
 */
public class StatusLog {
	
	/** Singleton object */
	private static StatusLog sStatusLog;
	
	/** Reference to Log TextView */
	private TextView mLogText = null;
	
	/**
	 * Constructor
	 */	
	private StatusLog(Activity activity) {
		// Get reference to log control
		mLogText = (TextView)activity.findViewById(R.id.logText);
	}
	
	/**
	 * Creates the Singleton object for StatusLog class
	 */
	public static void createLog(Activity activity) {
		sStatusLog = new StatusLog(activity);
	}
	
	/**
	 * Returns the reference to singleton object
	 */
	public static StatusLog updateLog() {
		return sStatusLog;
	}
	
	/**
	 * Updates the status message
	 */
	public void updateStatusMsg(String msg) {
		mLogText.setTextColor(Color.WHITE);
		mLogText.setText(msg);
	}
	
	/**
	 * Updates the error message in red color
	 */
	public void updateErrorMsg(String msg) {
		mLogText.setTextColor(Color.RED);
		mLogText.setText(msg);
	}
}
