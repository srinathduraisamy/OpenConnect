package vpn.openconnect.ui;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import org.apache.http.HttpVersion;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

/**
 * Class for Main dialog 
 * Extends Activity
 */
public class MainDialog extends Activity {

	// Member variables
	private Handler mCertificateHandler = null;
	private Handler mErrorLogHandler = null;
	private OpenConnectSSLSocketFactory mSslSocketFactory = null;
	private boolean mPermissionObtained = false;
	private boolean mSendRequest = false;
	private ComboBox mServerListComboBox;
	private String mServerAddress = "";
			
	/**
	 * Sets the Main dialog as the content view and creates an singleton object for StatusLog class.
	 */
	@Override
    public void onStart() { 
		super.onStart();
        
		// Error handler to send the error messages from the async thread
        mErrorLogHandler = new Handler() { 
        	public void handleMessage(Message msg) {
	            // Update the error message in the status log bar
	            StatusLog.updateLog().updateErrorMsg((String) msg.obj);
	        }
	    }; 
	    
        // Certificate handler to create an UI to accept or decline the server certificate
        mCertificateHandler = new Handler() {
	        
        	public void handleMessage(Message msg) { 
	        	if(msg.obj == null) { 
	        		// trusted server certificates send the get request.
	        		if(!mSendRequest) { 
	        			mSendRequest = true;
	        			// send the http server request
	        			sendHttpGetRequest();
	        		}
	        		return;
	        	}
	        	
	        	//Display the certificate and get user permission for adding the 
	        	//certificates to the key store
	        	if(!mPermissionObtained) getUserPermission((X509Certificate[]) msg.obj);
	        	else {
	        		if(mSslSocketFactory != null) {
	        			mSslSocketFactory.getTrustManagerRef().addCertificatesToKeyStore((X509Certificate[]) msg.obj);
	        		}
	        	}
	        }
	    };
	   	    
	    // Set the main dialog layout as the content view
        setContentView(R.layout.maindialoglayout);

        // Creates the singleton object for StatusLog class 
        StatusLog.createLog(this); 
        
        // ComboBox to display the server list from xml
	    mServerListComboBox = new ComboBox(this);
	    String[] serverListItems = mServerListComboBox.readServersListFromXml(this);
	    if(serverListItems != null) mServerListComboBox.setSuggestionSource(serverListItems);
        
        // Add the combo box to the layout
        RelativeLayout contentLayout = (RelativeLayout) findViewById(R.id.contentLayout);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
	    layoutParams.alignWithParent = true;
	    layoutParams.leftMargin = 15;
	    layoutParams.addRule(RelativeLayout.RIGHT_OF, R.id.serverLabel);
	    contentLayout.addView(mServerListComboBox, layoutParams);

        try { 
        	
        	// Create a ssl socket and trust manager to load the client and store certificates 
	    	mSslSocketFactory = new OpenConnectSSLSocketFactory(this, mCertificateHandler);
	    	
        } catch (NoSuchAlgorithmException e) {
			StatusLog.updateLog().updateErrorMsg("Trust manager error" + e.getMessage());
        } catch (KeyStoreException e) {
        	StatusLog.updateLog().updateErrorMsg("Key Store exception" + e.getMessage());
        }
	}
	 
	/**
	 * Executes on connect button click event.
	 * Gets the entered server address and sends the HttpGet request using HttpClient class
	 */
	public void onConnectClicked(View view) {
    	
		String sslAddress = "";
		
		// Get the server address
		mServerAddress = mServerListComboBox.getServerAddress();
		
		// Validate server address
		if(mServerAddress.length() == 0) {
			StatusLog.updateLog().updateErrorMsg("Please enter server address");
		} else {
			
			// validate the input server address.
			if(!mServerAddress.startsWith("http")) {
				sslAddress = mServerAddress;
				mServerAddress = "https://" + mServerAddress;
			} else {
				if(mServerAddress.startsWith("https://")) {
					sslAddress = mServerAddress.split("//")[1];
				} else {
					StatusLog.updateLog().updateErrorMsg("Only https:// permitted for server URL");
					return;
				}
			}
			
			// Set to default.
			mPermissionObtained = false;
						
			// Perform the SSL handshake, certificate validation in the background
			SslHandShakeAsync sslhandShakeAsync = new SslHandShakeAsync(this, mSslSocketFactory, mErrorLogHandler);
			sslhandShakeAsync.execute(sslAddress);
			
		}
	}
	
	/**
	 * Get the user permission for adding the server certificate to the key store
	 */
	public void getUserPermission(final X509Certificate[] certificates) {
		 
		// Create and display the untrusted certificate warning to the user.
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage("UnTrusted certificate. Do you want to add it in key store and continue") 
    	       .setCancelable(false)
    	       
    	       .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	    	   // On yes button clicked
    	    	   public void onClick(DialogInterface dialog, int id) { 
    	    		   mPermissionObtained = true;
    	    		   // Add the server to the key store
    	        	   mSslSocketFactory.getTrustManagerRef().addCertificatesToKeyStore(certificates);
    	        	   // Sends Http get request
    	        	   sendHttpGetRequest(); 
    	    	   } 
    	       })
    	       
    	       .setNeutralButton("View", new DialogInterface.OnClickListener() {
    	    	   // On view button clicked
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   //Display the certificate details to the user
    	        	   displayCertificate(certificates);
    	           }
    	       })
    	       
    	       .setNegativeButton("No", new DialogInterface.OnClickListener() {
    	    	   // On No button clicked
    	           public void onClick(DialogInterface dialog, int id) {
    	           }
    	       }); 
    	
    	AlertDialog alert = builder.create();
    	alert.show();
    } 
	
	/**
	 * Display the certificate and get the user permission for adding the server certificate to the key store
	 */
	public void displayCertificate(final X509Certificate[] certificates) { 
		
		//Get certificate details
		StringBuilder certificateString = new StringBuilder();
		if(certificates.length > 0) {
			
			certificateString.append(certificates[0].getSubjectDN().toString()).append("\n");
			certificateString.append("Issuer: ").append(certificates[0].getIssuerDN().toString()).append("\n");
			
		} else {
			certificateString.append("No Valid certificates");
		}
		
		// Dialog to display the certificate and to get permission
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
    	builder.setMessage(certificateString)
    	       .setCancelable(false)
    	       
    	       .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
    	    	   // On Accept button clicked
    	           public void onClick(DialogInterface dialog, int id) {
    	        	   mPermissionObtained = true;
    	        	   mSslSocketFactory.getTrustManagerRef().addCertificatesToKeyStore(certificates);
    	        	   sendHttpGetRequest();
    	           }
    	       })
    	       
    	       .setNegativeButton("Decline", new DialogInterface.OnClickListener() {
    	    	   // On Decline button clicked
    	    	   public void onClick(DialogInterface dialog, int id) {
    	    	   }
    	       });
    	
    	AlertDialog alert = builder.create();
    	alert.show();
    }

	/** 
	 * Send the http get request to the server
	 */
	public void sendHttpGetRequest() { 
		
		// Creates new http params
		HttpParams httpParams = new BasicHttpParams(); 
		HttpProtocolParams.setVersion(httpParams, HttpVersion.HTTP_1_1); 
		HttpProtocolParams.setContentCharset(httpParams, HTTP.UTF_8);
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000); 
		HttpConnectionParams.setSoTimeout(httpParams, 5000);
		
		SchemeRegistry registry = new SchemeRegistry(); 
		registry.register(new Scheme("https", mSslSocketFactory, 443)); 
		ClientConnectionManager connectionManager = new ThreadSafeClientConnManager(httpParams, registry);
		
		// Create an object of HttpClient and send Http get request 
		HttpClient httpClient = new HttpClient(this, connectionManager, httpParams, mErrorLogHandler); 
		httpClient.sendHttpGetRequest(mServerAddress);
	}
}
