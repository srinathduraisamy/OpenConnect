package vpn.openconnect.ui;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Sends Http requests in background.
 * Extends AsyncTask
 */ 
public class HttpRequestAsync extends AsyncTask<HttpUriRequest, Integer, HttpResponse> {
   
	private HttpClient mHttpClient;
	private HttpContext mHttpContext;
	private Handler mErrorLogHandler;
	private ProgressDialog mDownloadProgressBar;
	
	/**
	 *  Constructor
	 */
	HttpRequestAsync(Activity activity, HttpClient httpClient, HttpContext httpContext, Handler errorLogHandler) {
		
		// Initialize the member variables
		mHttpClient      = httpClient;
		mHttpContext     = httpContext;
		mErrorLogHandler = errorLogHandler; 

		// Create and initialize progress bar
		mDownloadProgressBar = new ProgressDialog(activity);
		mDownloadProgressBar.setMessage("Loading");
		mDownloadProgressBar.setIndeterminate(true);
		mDownloadProgressBar.setCancelable(false);
	}
		
	/**
	 * Sends the input Http request in background thread
	 */
	@Override
	protected HttpResponse doInBackground(HttpUriRequest... request) {
		
		try {
			
			// Publish the progress
			publishProgress(0);
			// Send the Http Request
			return mHttpClient.execute(request[0], mHttpContext);
			
		} catch (ClientProtocolException e) {
			
			// Send the error log to UI thread to update in the dialog
			Message errorMsg = mErrorLogHandler.obtainMessage();
			errorMsg.obj = e.getMessage();
			mErrorLogHandler.sendMessage(errorMsg);
			
		} catch (IOException e) {
			
			// Send the error log to UI thread to update in the dialog
			Message errorMsg = mErrorLogHandler.obtainMessage();
			errorMsg.obj = e.getMessage();
			mErrorLogHandler.sendMessage(errorMsg);

		}

		return null;
	}
	
	/**
	 * Runs on the UI thread.
	 * Starts a progress dialog and displays it.
	 */
	protected void onProgressUpdate (Integer... values) {
		super.onProgressUpdate(values[0]);
		mDownloadProgressBar.show();
	}
	
	/**
	 * Gets the return value returned by doInBackground() and calls received response of HttpClient
	 */
	protected void onPostExecute (HttpResponse response) {
		super.onPostExecute(response);
		
		mDownloadProgressBar.hide();
		mHttpClient.receivedResponse(response);
	}

}
