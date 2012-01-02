package vpn.openconnect.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Class to create Dialog from the input xml contents.
 */
public class DialogFromXml  {
   
	// Member variables
	private final String USERNAME       = "username";
	private final String PASSWORD       = "password";
	private EditText mUsernameEditText  = null;
	private EditText mPasswordEditText  = null;
	private Node mGroupNode             = null;
	private String mFormMessage         = null;
	private String mErrorMessage        = null;
	private String mWebVpn              = null;
	private int mGroupSelectionPos      = 0;
	private Activity mMainActivity;
	private HttpClient mHttpClient;
	
	/**
     * Constructor
     */
	DialogFromXml(Activity mainActivity, HttpClient client) {
				
		// Initialize member variables
		mMainActivity = mainActivity;
		mHttpClient   = client;
	}
	
	/**
	 * Class to handle Spinner item select event.
	 * Implements OnItemSelectedListnenr
	 */ 
    class GroupSpinnerSelectedListener implements OnItemSelectedListener {
     	
    	/**
    	 * Gets executed on spinner item selected event.
    	 */
		public void onItemSelected(AdapterView<?> parent, View v, int pos, long row) {
			// Store the selected group pos.
			DialogFromXml.this.mGroupSelectionPos = pos;
		}

		/** overriding the OnItemSelectedListener Interface method */
		public void onNothingSelected(AdapterView<?> arg0) {}
	}
    
    /**
	 * Class to handle Submit button click event.
	 * Implements OnClickListener
	 */
    class SubmitButtonOnClickListener implements View.OnClickListener {
     	
    	/**
    	 * Execute on submit button click event.
    	 * Generates HttpPost request's contents based on user name, password
    	 * and spinner inputs
    	 */
    	public void onClick(View view) {
			
    		// Generate post request on selected group and user name and password input.
			String postRequestMsg = "";
			
			if(mGroupNode != null) {
				// Add the selected group value
				postRequestMsg += mGroupNode.getAttributes().getNamedItem("name").getNodeValue();
				postRequestMsg += "=";
				// Gets reference to selected options node.
				Node selectedNode = mGroupNode.getChildNodes().item(DialogFromXml.this.mGroupSelectionPos+1);
				postRequestMsg += selectedNode.getAttributes().item(0).getNodeValue();
				postRequestMsg += "&";
			}
			
			if(mUsernameEditText != null) {
				// Add the entered user name
				postRequestMsg += USERNAME + "="; 
				postRequestMsg += mUsernameEditText.getText().toString();
				postRequestMsg += "&";				
			}
			
			if(mPasswordEditText != null) {
				// Add the entered password
				postRequestMsg += PASSWORD + "=";
				postRequestMsg += mPasswordEditText.getText().toString();				
			}
			
			// Send the generated Http post request
			mHttpClient.sendHttpPostRequest(postRequestMsg);
		}
    }
    
    /**
	 * Class to handle Reset button click event.
	 * Implements OnClickListener
	 */
    class ResetButtonOnClickListener implements View.OnClickListener {
    	
    	/**
    	 * Executes on reset button click event.
    	 */
    	public void onClick(View view) {
    		// Clear the controls
			if(mUsernameEditText != null) mUsernameEditText.setText("");
			if(mPasswordEditText != null) mPasswordEditText.setText("");
		}
    }
        
	/**
	 * Parse the xml response received for the server
	 */ 
	public int parseXmlResponse(Node rootNode, Header[] headers) {
		
		// Authentication succeed 
		if(rootNode.getAttributes().item(0).getNodeValue().compareTo("success") == 0) {

			// Retrieve the webvpn cookie 
			for(int i = 0; i < headers.length; i++) { 
				
				if(headers[i].getElements()[0] != null) {
				
					if(headers[i].getElements()[0].getName().compareTo("webvpn") == 0) {
					
						mWebVpn = headers[i].getElements()[0].getValue();
						
						//Display the webvpn cookie in message box
						AlertDialog.Builder builder = new AlertDialog.Builder(mMainActivity); 
						builder.setMessage("WebVPN Cookie:" + mWebVpn) 
						        .setCancelable(false)
						    	.setPositiveButton("Ok", new DialogInterface.OnClickListener() { 
						    		// On yes button clicked 
						    		public void onClick(DialogInterface dialog, int id) {     		    
						    		} }); 
						AlertDialog alert = builder.create();
						alert.show(); 
					
					} else if(headers[i].getElements()[0].getName().compareTo("webvpnc") == 0) {
					
						// Retrieve the webvpnc cookies
						String bu = null;
						String fu = null;
						String sh = null;
						
						String[] cookies = headers[i].getElements()[0].getValue().split("&");
						
						for(int j = 0; j < cookies.length; j++) {
							
							String[] tokens = cookies[j].split(":");
							if(tokens.length == 2) {
								if(tokens[0].compareTo("bu") == 0) bu = tokens[1];
								if(tokens[0].compareTo("fu") == 0) fu = tokens[1];
								if(tokens[0].compareTo("fh") == 0) {
									
									if(compareSha1(tokens[1]) == false )
										sh = tokens[1];
								}
							}
						}
							
						if(bu != null && fu != null && sh != null) 	
							fetchConfig(mWebVpn, bu, fu, sh);
					}
				}
			}
								
			return 2;
		}
			
		// Iterate through the xml nodes
		for(Node currentNode = rootNode.getFirstChild(); currentNode != null; currentNode = currentNode.getNextSibling()) {
			
			if( currentNode.getNodeType() != Node.ELEMENT_NODE) continue;
			
			if(mFormMessage == null) {
				if(currentNode.getNodeName().compareTo("message") == 0) {
					mFormMessage = currentNode.getFirstChild().getNodeValue();
				}
			}
								
			if(mErrorMessage == null) {
				if(currentNode.getNodeName().compareTo("error") == 0) {
					mErrorMessage = currentNode.getFirstChild().getNodeValue();
				}
			}
								
			if(currentNode.getNodeName().compareTo("form") == 0) parseForm(currentNode);
		}
		
		return 0;
	}
	
	/**
	 * Parse the form nodes in the xml response
	 */
	private void parseForm(Node formNode) {
		
		String str;
		// Create a layout
		TableLayout formLayout  = new TableLayout(mMainActivity);
		formLayout.setGravity( 0x10 );
		
		// add the form message to the layout
		if(mFormMessage != null) {
			
			TextView view = new TextView(mMainActivity);
			view.setText(mFormMessage);
			formLayout.setPadding(0, 25, 0, 0);
			formLayout.addView(view);
		}
		 
		// Iterate through form's child nodes
		for(Node currentNode = formNode.getFirstChild(); currentNode != null; currentNode = currentNode.getNextSibling()) {
			
			if( currentNode.getNodeType() != Node.ELEMENT_NODE) continue;
			
			// If form contains group
			if(currentNode.getNodeName().compareTo("select") == 0) {
				
				mGroupNode = currentNode;
				str = currentNode.getAttributes().item(1).getNodeValue();
				if( str != null ) {
					
					TextView view = new TextView(mMainActivity);
					view.setText(str);
					formLayout.setPadding(0, 10, 0, 0);
					formLayout.addView(view);
				}
				
				// Get the select group values and add it in the spinner
				NodeList childNodeList = mGroupNode.getChildNodes();
				if(childNodeList != null) {
					
					Spinner spinner = new Spinner(mMainActivity);
					formLayout.addView(spinner); 
				
					String[] items = new String[childNodeList.getLength() - 1 ];
					for( int i = 1; i < childNodeList.getLength(); i++ ) {
						items[i - 1] = childNodeList.item(i).getFirstChild().getNodeValue();
					}
										
					ArrayAdapter<String> adapter = new ArrayAdapter<String>(mMainActivity,
							android.R.layout.simple_spinner_item, items);
					adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
					spinner.setAdapter(adapter);
					
					// add the on item selected listener to the spinner					
					OnItemSelectedListener spinnerListener  = new GroupSpinnerSelectedListener();
					spinner.setOnItemSelectedListener(spinnerListener);
				}
			}
			
			// Create the controls
			if(currentNode.getNodeName().compareTo("input") == 0) {
				
				str = currentNode.getAttributes().getNamedItem("type").getNodeValue();
				if(str.compareTo("submit") == 0) {
					
					Button submitButton = new Button(mMainActivity);
					submitButton.setText(currentNode.getAttributes().getNamedItem("value").getNodeValue());
					formLayout.setPadding(0, 10, 0, 0);
					formLayout.addView(submitButton);
					
					SubmitButtonOnClickListener submitButtonOnClickListener = new SubmitButtonOnClickListener();
					submitButton.setOnClickListener( submitButtonOnClickListener );
					
				} else if(str.compareTo("reset") == 0) {
					
					Button resetButton = new Button(mMainActivity);
					resetButton.setText(currentNode.getAttributes().getNamedItem("value").getNodeValue());
					formLayout.setPadding(0, 10, 0, 0);
					formLayout.addView(resetButton);
		
					ResetButtonOnClickListener resetButtonOnClickListener = new ResetButtonOnClickListener();
					resetButton.setOnClickListener(  resetButtonOnClickListener );
					
				} else if(str.compareTo("text") == 0) {
					
					if(currentNode.getAttributes().getNamedItem("name").getNodeValue().compareTo(USERNAME) == 0) {
						
						TextView view = new TextView(mMainActivity);
						view.setText(currentNode.getAttributes().getNamedItem("label").getNodeValue());
						formLayout.setPadding(0, 10, 0, 0);
						formLayout.addView(view);
					
						mUsernameEditText = new EditText(mMainActivity);
						mUsernameEditText.setSingleLine();
						formLayout.addView(mUsernameEditText);
					}
					
				} else if(str.compareTo(PASSWORD) == 0) {
					
					TextView view = new TextView(mMainActivity);
					view.setText(currentNode.getAttributes().getNamedItem("label").getNodeValue());
					formLayout.setPadding(0, 10, 0, 0);
					formLayout.addView(view);
					
					mPasswordEditText = new EditText(mMainActivity);
					mPasswordEditText.setSingleLine();
					mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
					formLayout.addView(mPasswordEditText);
				}
			}
		}
		
		// Add the created layout and controls into main layout
		addFormToMainLayout(formLayout);
		
		// If the form contain error message display it in the log layout
		if(mErrorMessage != null) {
			StatusLog.updateLog().updateErrorMsg(mErrorMessage); 
		}
 	}

	/**
	 *  Clears the old layout and adds the newly created layout to the main layout  
	 */
	private void addFormToMainLayout(TableLayout formLayout) {

		// Get reference to main and log layout
		RelativeLayout mainLayout = (RelativeLayout)mMainActivity.findViewById(R.id.mainLayout);
		RelativeLayout logLayout  = (RelativeLayout)mMainActivity.findViewById(R.id.logLayout);
		
		// Clear the old layouts
		mainLayout.removeAllViewsInLayout();
					
		// Add the new form layout
		TableLayout.LayoutParams params = new TableLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		mainLayout.addView(formLayout, params);
		mainLayout.addView(logLayout);
	}
	
	/**
	 * Fetches the configuration xml file from the server
	 */
	public void fetchConfig(String webVpn, String bu, String fu, String sh) {
		
		// Add the cookie values
		StringBuilder cookie = new StringBuilder("webvpn=" + webVpn + "; ");
		cookie.append("webvpnc=bu:" + bu + "; "); 
		cookie.append("webvpnaac=1");
				
		// Create the HttpGet request
		HttpGet httpGet = new HttpGet(mHttpClient.getServerAddress() + bu + fu);
		httpGet.setHeader("Accept", "*/*");
		httpGet.setHeader("Accept-Encoding", "identity");
		httpGet.setHeader("Cookie", cookie.toString());
		httpGet.setHeader("X-Transcend-Version", "1");
			
		// Send the request async
		mHttpClient.sendHttpGetRequest(httpGet);
	
	}
	
	/**
	 * Compare the Sha1 of the existing configuration file and the Sha1 of the downloading one  
	 */
	boolean compareSha1(String downloadedSha1) {
	
		StringBuilder message = new StringBuilder();;
		
		try {
			
			// Calculate the Sha1 of the config.xml file
			InputStream fileInputStream = mMainActivity.openFileInput("config.xml");
			
			// Read the data from the file
			char[] buffer = new char[100];
			Reader inputStreamReader = new InputStreamReader(fileInputStream, "UTF-8");
			int dataRead;
			
			do {
				
				dataRead = inputStreamReader.read(buffer, 0, buffer.length);
				if (dataRead > 0) {
					message.append(buffer, 0, dataRead);
				}
				
			} while (dataRead >= 0);
			
		} catch	(FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		if(message.toString().length() > 0) {
			
			// Calculate the Sha1
			String fileSha1Hash = Sha1.computeSha1OfString(message.toString());
					
			// Compare the sha1 of the config.xml and retrieved sha1
			if( fileSha1Hash.compareTo(downloadedSha1.toLowerCase()) == 0 ) return true;
		}
		
		return false;
	}
}
