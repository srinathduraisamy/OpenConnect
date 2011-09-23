package vpn.openconnect.ui;

import android.app.Activity;
import android.view.View;
import android.widget.EditText;

/**
 * Class for Main dialog 
 * Extends Activity
 */
public class MainDialog extends Activity {
	
	/**
	 * Sets the Main dialog as the content view and creates an singleton object for StatusLog class.
	 */
	@Override
    public void onStart() {
        super.onStart();
    	
        // Set the maindialog layout as the content view
        setContentView(R.layout.maindialoglayout);
        // Creates the singleton object for StatusLog class 
        StatusLog.createLog(this);
	}
	 
	/**
	 * Executes on connect button click event.
	 * Gets the entered server address and sends the HttpGet request using HttpClient class
	 */
	public void onConnectClicked(View view) {
    	
		// Get the server address
		EditText serverText = (EditText)findViewById(R.id.serverText);
		String serverAddress = serverText.getText().toString();
						
		if(serverAddress.length() == 0) {
			StatusLog.updateLog().updateErrorMsg("Please enter server address");
		} else {
			
			// TODO: Update the validation by using URL class.
			// validate the input server address.
			if(!serverAddress.startsWith("http")) {
				serverAddress = "https://" + serverAddress;
			} else if(!serverAddress.startsWith("https://")) {
				StatusLog.updateLog().updateErrorMsg("Only https:// permitted for server URL");
				return;
			}
					
			// Create an object of HttpClient and send Http get request
			HttpClient httpClient = new HttpClient(this);
			httpClient.sendHttpGetRequest(serverAddress);
		}
    }
}

