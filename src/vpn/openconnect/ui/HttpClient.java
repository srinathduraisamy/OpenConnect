package vpn.openconnect.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class for Http functionalities.
 * Extends DefaultHttpClient.
 */
public class HttpClient extends DefaultHttpClient {

	// Member variable
	private Activity mMainActivity;
	private HttpContext mHttpContext;
	private Handler mErrorLogHandler;
	private String mServerAddress = null;
	
	/**
	 * Constructor
	 */
	HttpClient(Activity mainActivity, ClientConnectionManager ccm, HttpParams params, Handler errorHandler) {
		super(ccm, params);
		
		// Initialize member variables
		mMainActivity = mainActivity;
		mHttpContext  = new BasicHttpContext();
		mErrorLogHandler = errorHandler;
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
	
	public void sendHttpGetRequest(HttpGet httpGet) {
		
		// Send the request Async to retrieve configuration file.
        StatusLog.updateLog().updateStatusMsg("Sent Http Get request");
        HttpRequestAsync sendReqAsync  = new HttpRequestAsync(mMainActivity, this, mHttpContext, mErrorLogHandler);
        sendReqAsync.execute(httpGet);
	}

	/**
	 *  Creates and send the http post request
	 */
	public void sendHttpPostRequest(String postRequestMsg) {
		
		// Set the headers
		HttpPost httpPost = new HttpPost(getRedirectedURL());
    	httpPost.setHeader("Accept", "*/*");
    	httpPost.setHeader("Accept-Encoding", "identity");
    	httpPost.setHeader("Cookie", "webvpnlogin=1");
    	httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
    	httpPost.setHeader("X-Transcend-Version", "1");
    	
		try {
			// Send the post request async
			StringEntity postRequestEntity = new StringEntity(postRequestMsg);
			httpPost.setEntity(postRequestEntity);
			
			StatusLog.updateLog().updateStatusMsg("Sent Http Post  request");
			HttpRequestAsync sendReqAsync = new HttpRequestAsync(mMainActivity, this, mHttpContext, mErrorLogHandler);
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
				try {
					
					InputStream inputStream = response.getEntity().getContent();
					Reader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
					
					// Read the data from the InputStream
					char[] buffer = new char[100];
					StringBuilder readData = new StringBuilder();
					int read;
					
					do {
					  read = inputStreamReader.read(buffer, 0, buffer.length);
					  if (read>0) {
						  readData.append(buffer, 0, read);
					  }
					} while (read>=0);
															
					// Generate the doc for xml parsing
					DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			        DocumentBuilder docBuilder = factory.newDocumentBuilder();
			        InputStream is = new ByteArrayInputStream(readData.toString().getBytes());
					Document xmlDoc = docBuilder.parse(is);
			        															
					if(xmlDoc == null) {
						StatusLog.updateLog().updateErrorMsg("Not a valid XML response");
						return;
					}
						
					// Get the root node and validate it.
					Node rootNode = xmlDoc.getFirstChild();
					if(rootNode == null) {
						StatusLog.updateLog().updateErrorMsg("Not a valid XML response");
						return;
					}
					
					if( rootNode.getNodeType() != Node.ELEMENT_NODE) {
						StatusLog.updateLog().updateErrorMsg("Not a valid XML response");
						return;
					}
					
					if(rootNode.getNodeName().compareTo("auth") == 0) {
						
						//Generate the Dialog based on XML
						DialogFromXml dialogFromXml = new DialogFromXml(mMainActivity, this);
						int retVal = dialogFromXml.parseXmlResponse(rootNode, response.getAllHeaders());
						if(retVal == -1) {
							StatusLog.updateLog().updateErrorMsg("Error in parsing the xml");
						} else if(retVal == 2) {
							//TODO: For success case
						}
						
					} else if(rootNode.getNodeName().compareTo("AnyConnectProfile") == 0) {
												
				        //Write the configuration xml into a file. 
						FileOutputStream fileOutputStream = mMainActivity.openFileOutput("config.xml", Context.MODE_PRIVATE);
						OutputStreamWriter outputStreamWriter = new OutputStreamWriter(fileOutputStream); 
						// Write the string to the file
						outputStreamWriter.write(readData.toString());
						outputStreamWriter.flush();
						fileOutputStream.close();
						
					} else {	
						StatusLog.updateLog().updateErrorMsg("XML response has no valid root node" );
					}
					
				} catch (SAXException e) {
					StatusLog.updateLog().updateErrorMsg("Failed to parse server response");		
				} catch (IOException e) {
					StatusLog.updateLog().updateErrorMsg("IO exception");
				} catch (ParserConfigurationException e) {
					StatusLog.updateLog().updateErrorMsg("IllegalStateException");
				} 
				catch (IllegalStateException e) {
					StatusLog.updateLog().updateErrorMsg("IllegalStateException");
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
	
	/**
	 * Return the stored server address
	 */
	public String getServerAddress() {
		return mServerAddress;
	}
}
