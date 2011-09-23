package vpn.openconnect.ui;

import android.app.Activity;
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

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * Class to create Dialog from the input xml contents.
 */
public class DialogFromXml  {
   
	private final String USERNAME       = "username";
	private final String PASSWORD       = "password";
	private EditText mUsernameEditText  = null;
	private EditText mPasswordEditText  = null;
	private Node mGroupNode             = null;
	private String mFormMessage         = null;
	private String mErrorMessage        = null;
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
	public int parseXmlResponse(InputStream inputStream) {
		
		try {
			
			// Generate the doc for xml parsing
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = factory.newDocumentBuilder();
			Document xmlDoc = docBuilder.parse(inputStream);

			if(xmlDoc == null) return -1;
			
			// Get the root node
			Node rootNode = xmlDoc.getFirstChild().getNextSibling();
			if(rootNode == null) return -1;
			
			if(rootNode.getNodeName().compareTo("auth") != 0) {
				StatusLog.updateLog().updateErrorMsg("XML response has no \"auth\" root node" );
			}
			
			if(rootNode.getAttributes().item(0).getNodeValue().compareTo("success") == 0) {
				// TODO: Implement the functionality for success case
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
						mErrorMessage = currentNode.getAttributes().getNamedItem("param1").getNodeValue();
					}
				}
								
				if(currentNode.getNodeName().compareTo("form") == 0) parseForm(currentNode);
			}
			
		} catch (SAXException e) {
			StatusLog.updateLog().updateErrorMsg("Failed to parse server response");		
		} catch (IOException e) {
			StatusLog.updateLog().updateErrorMsg("IO exception");
		} catch (ParserConfigurationException e) {
			StatusLog.updateLog().updateErrorMsg("IllegalStateException");
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
					mUsernameEditText.setSingleLine();
					mPasswordEditText.setTransformationMethod(new PasswordTransformationMethod());
					formLayout.addView(mPasswordEditText);
				}
			}
		}
		
		// Add the created layout and controls into main layout
		addFormToMainLayout(formLayout);
		
		// If the form contain error meesage display it in the log layout
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
}
