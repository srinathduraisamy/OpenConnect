package vpn.openconnect.ui;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
 
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

/**
 * Trust manager validate the server certificate and load the certificates into key store
 * Implements X509TrustManager
 */
public class OpenConnectTrustManager implements X509TrustManager {
	
	/**
	 * BksFileFilter implements file filter to filter all the bks file in the application directory.
	 */
	class BksFileFilter implements FileFilter {

		// String contains the extension to be filtered
		private String[] fileExtension = {"bks", "cer"}; 
		
		@Override
		public boolean accept(File pathname) { 
			if(pathname.isDirectory()) {
				return true;
			}

			String name = pathname.getName().toLowerCase();
			for (String anExt : fileExtension) {
				if (name.endsWith(anExt)) {
					return true;
				}
			}
			return false;
		}
	}
 
	// Member variables
	private X509TrustManager mDefaultTrustManager;
    private Handler mCertificateHandler;
    private static KeyStore mKeyStore;
    private static X509TrustManager mLocalTrustManager;
    private Context mMainContext;
		
    /**
     * Constructor: Load the client certificate if any and the saved certificates into key store
     */
    public OpenConnectTrustManager(Context mainContext, Handler certificateHandler) throws NoSuchAlgorithmException, KeyStoreException {
        super();  
        
        mCertificateHandler = certificateHandler;
        mMainContext = mainContext;
        FileInputStream fileInputStream = null;
        
        try {
        	
        	boolean validKeyStore = false;
           	
        	// Get the instance of BKS key store
        	mKeyStore = KeyStore.getInstance("BKS");
        	
           	// Get the list of BKS file in the application's file directory
        	File[] bksFiles = mMainContext.getFilesDir().listFiles(new BksFileFilter());
           	// Iterate through the BKS files
        	for(File bksFile : bksFiles ) {
        		// Get the input stream 
           		fileInputStream = getFileInputStream(bksFile); 
            	if( fileInputStream != null) {
            		// Load the BKS file into key store
            		if(loadKeyStore(fileInputStream)) validKeyStore = true; 
            	}
           	}
           	
           	// If no valid key store then load the default. 
        	if(!validKeyStore) loadKeyStore(null);
        	
        	// Get Instance of trust manager 
        	TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
        	// Load the key store
        	trustManagerFactory.init(mKeyStore); 
        	       	
        	TrustManager[] trustManagers = trustManagerFactory.getTrustManagers(); 
        	if (trustManagers != null) { 
        		for (TrustManager trustManager : trustManagers) {
        			if (trustManager instanceof X509TrustManager) {
        				mLocalTrustManager = (X509TrustManager)trustManager;
        				break;
        			}
        		}
        	}
        
        	trustManagerFactory = TrustManagerFactory.getInstance("X509"); 
        	trustManagerFactory.init((KeyStore)null); 
        	
        	trustManagers = trustManagerFactory.getTrustManagers(); 
        	if (trustManagers != null) { 
        		for (TrustManager trustManager : trustManagers) {        	
        			if (trustManager instanceof X509TrustManager) {
        				mDefaultTrustManager = (X509TrustManager) trustManager;
        				break;
        			}
        		}
        	}
        	
        } catch(NoSuchAlgorithmException e) {
        	fileInputStream = null;
        	mKeyStore = null;
        	throw e;
        } catch(KeyStoreException e) {
        	throw e;
        }
    }
 
    /**
     * Overriding X509TrustManager method.
     */
    @Override
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    	mDefaultTrustManager.checkClientTrusted(certificates, authType);
    }
 
    /**
     * Validate the server certificate. If the certificate is not valid, send the certificate to 
     * gui thread for user acceptance
     */
    @Override
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
    	
    	Message errorMsg =  mCertificateHandler.obtainMessage();;
    	errorMsg.obj = null;
    	
    	try { 
    		
    		// Validate the server certificate
    		try { 
    			mDefaultTrustManager.checkServerTrusted(certificates, authType); 
    		} catch (CertificateException e) { 
    			mLocalTrustManager.checkServerTrusted(new X509Certificate[] {certificates[0]}, authType);
    		}
    		
    	} catch(CertificateException e) { 
        	// Add the untrusted certificates to send it to the gui thread.
    		errorMsg.obj = certificates;
        }
    	
    	// Sends the null message in case of valid certificates or the untrusted certificates to the gui thread
    	finally {
        	mCertificateHandler.sendMessage(errorMsg);
        }
        	
    }
    
    /**
     * Add the untrusted certificate to the key store
     */
    public void addCertificatesToKeyStore(X509Certificate[] certificates) { 
    	
    	// Add certificates to key store        		 
		try {
			
			TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("X509");
			
			// Add the certificates to the key store.
			for (X509Certificate certificate : certificates)
				mKeyStore.setCertificateEntry(certificate.getSubjectDN().toString(), certificate);
			
			trustManagerFactory.init(mKeyStore);
			TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
			
			if (trustManagers != null) {
				for (TrustManager trustManager : trustManagers) {
                   if (trustManager instanceof X509TrustManager) {
                	   mLocalTrustManager = (X509TrustManager) trustManager;
                       break;
                   }
               }
			}
			
			// Store the key store with the newly added certificate into a file
			FileOutputStream fileOutputStream  = mMainContext.openFileOutput("KeyStore.bks", Context.MODE_PRIVATE);
		    mKeyStore.store(fileOutputStream, "".toCharArray());
		    fileOutputStream.close();
		         
		} catch (KeyStoreException e) {
			StatusLog.updateLog().updateErrorMsg("Saving Certificate into key store: " + e.getMessage());
		} catch (IOException e) {
	        StatusLog.updateLog().updateErrorMsg("Saving Certificate into key store: " + e.getMessage());
		} catch (NoSuchAlgorithmException e) {
			StatusLog.updateLog().updateErrorMsg("Saving Certificate into key store: " + e.getMessage());
		} catch (CertificateException e) {
			StatusLog.updateLog().updateErrorMsg("Saving Certificate into key store: " + e.getMessage());
		}
    }
    
    /**
     * Load the certificate file into key store
     */
    public boolean loadKeyStore(FileInputStream fileInputStream) {
    	
    	try {
    		
        	mKeyStore.load(fileInputStream, "".toCharArray());
        	return true;
        	
        } catch (IOException e) {
        	StatusLog.updateLog().updateErrorMsg("Loading key store: " + e.getMessage());
        } catch (CertificateException e) {
        	StatusLog.updateLog().updateErrorMsg("Loading key store: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
        	StatusLog.updateLog().updateErrorMsg("Loading key store: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Get the input stream to the input file
     */
    public FileInputStream getFileInputStream(File file) {
    	
    	try {
        	return new FileInputStream(file);
        } catch (FileNotFoundException e) {
        	StatusLog.updateLog().updateErrorMsg("Loading key store: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get the accepted issuers
     */
    public X509Certificate[] getAcceptedIssuers() {
        return mDefaultTrustManager.getAcceptedIssuers();
    }
}
