package vpn.openconnect.ui;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.LinearLayout;

/**
 * 
 * Class for ComboBox Control
 */
public class ComboBox extends LinearLayout { 
	
	// Member variables
	private AutoCompleteTextView mTextView = null;
	private int mItemSelectedPos = -1;
	private NodeList mServersListNode = null;
		 	 
	// Constructor
	public ComboBox(Context context) { 
		super(context);
		// Creates the child controls for combo box
	 	createControls(context); 
	}
	
	class ItemsSelectedListener implements AdapterView.OnItemClickListener { 

		@Override
		/**
		 * Called when an item gets selected.
		 */
        public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long arg3) {
			ComboBox.this.mItemSelectedPos = pos;
		}
	}
		
	/**
	 * Extend the array adapter to stop displaying the suggestions. 
	 */
	class UnconditionalArrayAdapter<T> extends ArrayAdapter<T> {
	    public UnconditionalArrayAdapter(Context context, int textViewResourceId, T[] objects) {
	        super(context, textViewResourceId, objects);
	    }

	    public android.widget.Filter getFilter() {
	    	return new NullFilter();
	    }
	    
	    class NullFilter extends android.widget.Filter {

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				
				return null;
			}

			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				return;
				
			}
	    }
	}
	
	/**
	 * Create the controls for combo box 
	 */
	private void createControls(Context context) {
		
		setOrientation(HORIZONTAL);
		setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
		mTextView = new AutoCompleteTextView(context);
		mTextView.setSingleLine();
		mTextView.setOnItemClickListener(new ItemsSelectedListener());
		
		addView(mTextView, new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT, 1));
		
		ImageButton dropDownButton = new ImageButton(context);
		dropDownButton.setImageResource(android.R.drawable.arrow_down_float);
		//Listener for drop down button
		dropDownButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) 
			{
				mTextView.showDropDown();
			}
		});
		
		this.addView(dropDownButton, new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT));
	}
	
	/**
	 * Sets the item to be displayed 
	 */
	public void setSuggestionSource(String[] items) 
	{
		ArrayAdapter<String> adapter = new UnconditionalArrayAdapter<String>(this.getContext(),
				android.R.layout.simple_spinner_item, items);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mTextView.setAdapter(adapter);
	}

	/**
	 * Reads the servers details from the xml file and return it as an array of string
	 */
	public String[] readServersListFromXml(Context mMainContext) {
		
		try {
	        
			InputStream fileInputStream = mMainContext.openFileInput("config.xml");
			
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder;
	        docBuilder = docBuilderFactory.newDocumentBuilder();
	        Document doc = docBuilder.parse(fileInputStream);
			
			// Get the servers list node.
			mServersListNode = doc.getElementsByTagName( "HostEntry" );
			
			if(mServersListNode != null) {
				
				String[] serversList = new String[mServersListNode.getLength()];
				
				// Iterate through the node and retrieve the server list.
				for(int i = 0; i < mServersListNode.getLength(); i++) {
					
					Node childNode = mServersListNode.item(i).getFirstChild();
					
					// Iterate through the child node
					for(Node currentNode = childNode; currentNode != null; currentNode = currentNode.getNextSibling()) {
						if( currentNode.getNodeType() != Node.ELEMENT_NODE) continue;
						
						// Add the server name to the array
						if(currentNode.getNodeName().compareTo("HostName") == 0) {
							serversList[i] = currentNode.getFirstChild().getNodeValue(); 
						}
					}
				}
				
				return serversList;
			}
			
        } catch (ParserConfigurationException e) {
        	e.getMessage();
		} catch (FileNotFoundException e) {
			e.getMessage();
        } catch (SAXException e) {
        	e.getMessage();
        } catch (IOException e) {
        	e.getMessage();
        }
		
		return null;
	}
	
	/**
	 * Retrieve the ip address for the selected server  
	 */
	public String getServerAddress() {  
		
		String textViewText = mTextView.getText().toString();
		
		// If not items selected return the entered text.
		if(mItemSelectedPos == -1) return textViewText;
		else {
			
			// Return the ip address of the selected item
			Node childNode = mServersListNode.item(mItemSelectedPos).getFirstChild();
			
			for(Node currentNode = childNode; currentNode != null; currentNode = currentNode.getNextSibling()) {
				
				if( currentNode.getNodeType() != Node.ELEMENT_NODE) continue;
				
				if(currentNode.getNodeName().compareTo("HostName") == 0) {
					
					if(currentNode.getFirstChild().getNodeValue().compareTo(textViewText) == 0) continue;
					else return textViewText; 
				}
				
				if(currentNode.getNodeName().compareTo("HostAddress") == 0) {
					return currentNode.getFirstChild().getNodeValue(); 
				}
			}
		}
		
		return null;
	}
}
	  
    
