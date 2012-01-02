package vpn.openconnect.ui;

import java.io.IOException;

import javax.net.ssl.SSLSocket;

import org.apache.http.HttpVersion;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;

/**
 *  SSLHandShakeAsync performs the SSL handshake with the vpn server
 *  in the background 
 */
public class SslHandShakeAsync extends AsyncTask<String, Integer, Integer> {

	// Member variables
	private ProgressDialog mDownloadProgressBar = null;
	private OpenConnectSSLSocketFactory mSslSocketFactory;
	private Handler mErrorLogHandler;
	
	// Constructor
	SslHandShakeAsync(Activity activity, OpenConnectSSLSocketFactory factory, Handler errorHandler) { 
		
		mSslSocketFactory = factory;
		mErrorLogHandler  = errorHandler;
		
		// Create and initialize progress bar
		mDownloadProgressBar = new ProgressDialog(activity);
		mDownloadProgressBar.setMessage("Loading");
		mDownloadProgressBar.setIndeterminate(true);
		mDownloadProgressBar.setCancelable(false);
	}
	
	/**
	 * Perform the SSL handshake in the background
	 */ 
	@Override
    protected Integer doInBackground(String... serverAddress) { 
		
		// Publish the progress
		publishProgress(0);
		
		// Set the http params
		HttpParams httpParams = new BasicHttpParams(); 
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1); 
		HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, 5000);
		
		try {
			
			// Start the handshake in the background 
			SSLSocket sslSocket = (SSLSocket) mSslSocketFactory.connectSocket(null, serverAddress[0], 443, null, 0, httpParams);
			sslSocket.startHandshake();
			        
		} catch (IOException e) {
			mDownloadProgressBar.cancel();
        	Message errorMsg = mErrorLogHandler.obtainMessage();
			errorMsg.obj = e.getMessage();
			mErrorLogHandler.sendMessage(errorMsg);
        } catch (Exception e) {
        	mDownloadProgressBar.cancel();
        	Message errorMsg = mErrorLogHandler.obtainMessage();
			errorMsg.obj = e.getMessage(); 
			mErrorLogHandler.sendMessage(errorMsg);
        }
       
        return 0;
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
	 * Executed on completing the handshake.
	 */
	protected void onPostExecute(Integer value) {
		super.onPostExecute(value);
		mDownloadProgressBar.cancel();
	}
}
