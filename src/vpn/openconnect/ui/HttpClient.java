package vpn.openconnect.ui;

import android.app.Activity;
import android.os.Handler;
import android.os.Message;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

/**
 * Class for Http functionalities.
 * Extends DefaultHttpClient.
 */
public class HttpClient extends DefaultHttpClient {
	
	private Activity mMainActivity;
	private HttpContext mHttpContext;
	private final Handler mErrorLogHandler;	
	private String mServerAddress = null;
	
	/**
	 * Constructor
	 */
	HttpClient(Activity mainActivity) {
		
		// Initialize member variables
		mMainActivity = mainActivity;
		mHttpContext  = new BasicHttpContext();

		// Handler for getting events for Http Async thread and updates the error log
		mErrorLogHandler = new Handler() {
	        public void handleMessage(Message msg) {
	            String errorMsg = (String) msg.obj;
	            StatusLog.updateLog().updateErrorMsg(errorMsg);
	        }
	    };
	}
	
	/**
	 * Creates and send the Http get requests
	 */	
	public void sendHttpGetRequest(String hostUrl) {
		
		mServerAddress  = hostUrl;
				
		// Set the headers
    	HttpGet httpGet = new HttpGet(mServerAddress);
        httpGet.setHeader("Accept", "*/*");
        httpGet.setHeader("Accept-Encoding", "identity");
        httpGet.setHeader("X-Transcend-Version", "1");
        
        // Send the request Async
        StatusLog.updateLog().updateStatusMsg("Sent Http Get request");
        HttpRequestAsync sendReqAsync  = new HttpRequestAsync(mMainActivity, this, mHttpContext, mErrorLogHandler);
        sendReqAsync.execute(httpGet);
	}

	/**
	 *  Creates and send the http post request
	 */
	public void sendHttpPostRequest(String postRequestMsg) {
		
		// Set the headers
		HttpPost httpPost = new HttpPost(mServerAddress);
    	httpPost.setHeader("Accept", "*/*");
    	httpPost.setHeader("Accept-Encoding", "identity");
    	httpPost.setHeader("Cookie", "webvpnlogin=1");
    	httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
    	httpPost.setHeader("X-Transcend-Version", "1");
    	
		try {
						
			StringEntity postRequestEntity = new StringEntity(postRequestMsg);
			httpPost.setEntity(postRequestEntity);
			
			StatusLog.updateLog().updateStatusMsg("Sent Http Post  request");
			HttpRequestAsync sendReqAsync  = new HttpRequestAsync(mMainActivity, this, mHttpContext, mErrorLogHandler);
	        sendReqAsync.execute(httpPost);
	        
		} catch (IOException e) {
			StatusLog.updateLog().updateErrorMsg("IO exception");
		} catch (IllegalStateException e) {
			StatusLog.updateLog().updateErrorMsg("Illegal State Exception");
		}
	}
	
	/**
	 * Callback when you get response for the Http request
	 * Validate the response and pass it for parsing the response.
	 */
	public void receivedResponse(HttpResponse response) {
		
		if(response != null) {
			
			if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
				StatusLog.updateLog().updateErrorMsg(response.getStatusLine().toString());
			} else {     	
				
				StatusLog.updateLog().updateStatusMsg("Got HTTP Response");
				// Get the host's redirected URL
				mServerAddress =  getRedirectedURL();
							
				try {
					
					// Generate the Dialog based on XML
					DialogFromXml dialogFromXml = new DialogFromXml(mMainActivity, this);
					int retVal = dialogFromXml.parseXmlResponse(response.getEntity().getContent());
					
					if(retVal == -1) {
						StatusLog.updateLog().updateErrorMsg("Not a valid XML response");
					} else if(retVal == 2) {
						//TODO: For success case
					}
					
				} catch (IllegalStateException e) {
					StatusLog.updateLog().updateErrorMsg("IllegalStateException");
				} catch (IOException e) {
					StatusLog.updateLog().updateErrorMsg("IO exception");	
				}
			}
		}
	}
	
	/**
	 * Get the redirected url from the response
	 */
	public String getRedirectedURL() {
		
		if(mHttpContext != null) { 
			
			HttpUriRequest currentReq = (HttpUriRequest) mHttpContext.getAttribute(
					ExecutionContext.HTTP_REQUEST);
			HttpHost currentHost = (HttpHost)  mHttpContext.getAttribute( 
	                ExecutionContext.HTTP_TARGET_HOST);
			
			if(currentReq != null && currentHost != null)
				return currentHost.toURI() + currentReq.getURI();
			
		} 
		
		return null;
    }
}
