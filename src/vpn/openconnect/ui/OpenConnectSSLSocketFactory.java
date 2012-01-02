package vpn.openconnect.ui;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
 
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
 
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.LayeredSocketFactory;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.os.Handler;

/**
 * Factory class for creating SSL sockets
 * Implements SocketFactory and LayeredSocket Factory 
 */
public class OpenConnectSSLSocketFactory implements SocketFactory, LayeredSocketFactory {

	// Member variables
	private SSLContext mSslcontext = null;
    private OpenConnectTrustManager mTrustManager = null;
    
    // Constructor 
    OpenConnectSSLSocketFactory(Context context, Handler certificateHandler) throws NoSuchAlgorithmException, KeyStoreException {
    	
    	// Creates the Trust Manager
    	try {
	        mTrustManager = new OpenConnectTrustManager(context, certificateHandler);
        } catch (NoSuchAlgorithmException e) {
	        throw e;
        } catch (KeyStoreException e) {
	        throw e;
        }
    }
 
    /**
     * Creates and return a new SSLContext and initialize the custom trust manager
     */
    private static SSLContext createEasySSLContext(OpenConnectTrustManager trustManager) throws IOException {
    	
        try {
        	SSLContext context = SSLContext.getInstance("TLS");
        	context.init(null, new TrustManager[] { trustManager }, null);
            return context;
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
 
    /**
     * Creates and return a new SSLContext if it not already created
     */
    public SSLContext getSSLContext() throws IOException {
        if (mSslcontext == null) {
        	mSslcontext = createEasySSLContext(mTrustManager);
        }
        return mSslcontext;
    }
 
    /**
     * Create a new socket set timeout parameters and connect it to vpn server 
     */
    @Override
    public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress, int localPort,
            HttpParams httpParams) throws IOException, UnknownHostException, ConnectTimeoutException {
    	
    	// Get the server's ip address
        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        // Create the socket
        SSLSocket sslSock = (SSLSocket) ((sock != null) ? sock : createSocket());
        
        // Bind the socket to the local port or 0
        if ((localAddress != null) || (localPort > 0)) {
            
            if (localPort < 0) {
                localPort = 0; 
            }
            
            InetSocketAddress localInetAddress = new InetSocketAddress(localAddress, localPort);
            sslSock.bind(localInetAddress);
        }
 
        // Set the connection timeout and read timeout in milliseconds.
        sslSock.connect(remoteAddress, HttpConnectionParams.getConnectionTimeout(httpParams));
        sslSock.setSoTimeout(HttpConnectionParams.getSoTimeout(httpParams));
 
        // Return the created socket
        return sslSock;
     }
 
    /**
     * Creates and return a new SSL socket
     */
    @Override
    public Socket createSocket() throws IOException {
        return getSSLContext().getSocketFactory().createSocket();
    }
 
    /**
     * Creates an SSLSocket over the specified socket that is connected to the specified host at the specified port.
     */
    @Override
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException,
            UnknownHostException {
        return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
    }
 
    @Override
    public boolean isSecure(Socket socket) throws IllegalArgumentException {
        return true;
    }
    
    /**
     * Returns the reference to open connect trust manager      
     */
    public OpenConnectTrustManager getTrustManagerRef() {
    	return mTrustManager;
    }
}
