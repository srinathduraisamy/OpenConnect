package vpn.openconnect.ui;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Class for calculating the Sha1
 */
public class Sha1 {
	
	/**
	 * Convert the input byte array into hex decimal. 
	 */
	private static String convToHex(byte[] data) {
        
		StringBuilder hexValue = new StringBuilder();
        
		for (int i = 0; i < data.length; i++) {
            
			int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;
            
            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    hexValue.append((char) ('0' + halfbyte));
                else
                	hexValue.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            } while(two_halfs++ < 1);
        }
        
        return hexValue.toString();
    }
	
	/*
	 * Computes the Sha1 of the input String and converts into hex value 
	 */
	public static String computeSha1OfString(String message) {
        
		try {
        	
			MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
			messageDigest.update(message.getBytes(("UTF-8")));
            byte[] sha1 = messageDigest.digest();
            return convToHex(sha1);
            
        } catch (UnsupportedEncodingException ex) {
        	ex.printStackTrace();    
        } catch (NoSuchAlgorithmException e) {
	        e.printStackTrace();
        }
        
        return null;
	}
	
}
