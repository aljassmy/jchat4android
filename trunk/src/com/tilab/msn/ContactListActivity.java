package com.tilab.msn;



import jade.android.ConnectionListener;
import jade.android.JadeGateway;
import jade.core.AID;
import jade.core.Profile;
import jade.imtp.leap.JICP.JICPProtocol;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.ControllerException;
import jade.wrapper.StaleProxyException;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.location.Location;
import android.os.Bundle;
import android.view.Menu;
import android.view.Menu.Item;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;
import android.widget.TabHost.TabSpec;

import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayController;
import com.google.android.maps.Point;

public class ContactListActivity extends MapActivity implements ConnectionListener {
   
	private JadeGateway gateway;
	private final Logger myLogger = Logger.getMyLogger(this.getClass().getName());
	private TabHost mainTabHost;
	private ListView contactsListView;
	private MapController mapController;
	private OverlayController overlayCtrl;

	//Adapter for the contacts list
	private ContactListAdapter contactsAdapter;
	
	//MENUITEM CONSTANTS
	private final int MENUITEM_ID_MAPMODE=Menu.FIRST;
	private final int MENUITEM_ID_EXIT=Menu.FIRST+1;
	
	//NEEDED TAGS FOR THE TABHOST (to address them)
	private final String CONTACTS_TAB_TAG="ContactsTab";
	private final String MAPVIEW_TAB_TAG="MapViewTab";
	
	
	private ContactsUpdaterBehaviour updateBh;
	
	//Array of updaters
	private Map<String, ContactsUIUpdater> updaters;
	
	public  ContactsUpdaterBehaviour getUpdateBehaviour(){
		return updateBh;
	}
	
	private void initUI(){
		//Setup the main tabhost
        setContentView(R.layout.main);
        mainTabHost = (TabHost) findViewById(R.id.main_tabhost);
        mainTabHost.setup();
    
      //Fill the contacts tab
        TabSpec contactsTabSpecs = mainTabHost.newTabSpec(CONTACTS_TAB_TAG);
		contactsTabSpecs.setIndicator(getText(R.string.contacts_tab_name));
		contactsTabSpecs.setContent(R.id.content1);
		mainTabHost.addTab(contactsTabSpecs);
        
    	//Fill the map tab
		TabSpec mapTabSpecs = mainTabHost.newTabSpec(MAPVIEW_TAB_TAG);
		mapTabSpecs.setIndicator(getText(R.string.mapview_tab_name));
		mapTabSpecs.setContent(R.id.content2);
		mainTabHost.addTab(mapTabSpecs);
        
        
		//init the map view
		MapView mapView = (MapView) findViewById(R.id.myMapView);
		mapController = mapView.getController();
		mapController.zoomTo(15);
		overlayCtrl = mapView.createOverlayController();
		overlayCtrl.add(new ContactsPositionOverlay(), false);
	
		
		//Create the updater array
        updaters = new HashMap<String, ContactsUIUpdater>(2);
        updaters.put(CONTACTS_TAB_TAG, new ContactListUpdater(this)); 
        updaters.put(MAPVIEW_TAB_TAG, new MapUpdater(this));
	
        //set the default updater
		TilabMsnApplication myApp =  (TilabMsnApplication) getApplication();
		myApp.myBehaviour.setContactsUpdater(updaters.get(CONTACTS_TAB_TAG));
	
		//Select default tab
		mainTabHost.setCurrentTabByTag(CONTACTS_TAB_TAG);
		
		
	     //Set the handler for the click on the tab host
		mainTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {

					@Override
					public void onTabChanged(String arg0) {
						
						TilabMsnApplication myApp =  (TilabMsnApplication) getApplication();
						myLogger.log(Logger.FINER, "Tab was switched! Current tab is "+ arg0 + " Changing the updater...");
						
						//FIXME: THIS LOOKS LIKE AN ANDROID BUG!!!!
						//We should investigate!!!!!
						if (arg0 == null){
							// TODO Auto-generated method stub	
							myApp.myBehaviour.setContactsUpdater(updaters.get(CONTACTS_TAB_TAG));
							//This forced update could be dangerous!!!
							contactsAdapter.updateAdapter(ContactManager.getInstance().getMyContact().getLocation(), ContactManager.getInstance().getOtherContactList());
							contactsListView.setAdapter(contactsAdapter);
							
						} else {
							myApp.myBehaviour.setContactsUpdater(updaters.get(MAPVIEW_TAB_TAG));
							Location loc = ContactManager.getInstance().getMyContact().getLocation();
							int latitudeE6 = (int)(loc.getLatitude()*1E6);
							int longitudeE6 = (int)(loc.getLongitude()*1E6);
							Point p = new Point(latitudeE6,longitudeE6);
							
							mapController.centerMapTo(p, true);
							
						}
					}        	
		});

		contactsListView = (ListView) findViewById(R.id.contactsList);
		contactsAdapter = new ContactListAdapter(this);
		ContactManager.getInstance().readPhoneContacts(this);
		contactsAdapter.updateAdapter(ContactManager.getInstance().getMyContact().getLocation(), ContactManager.getInstance().getOtherContactList());
		contactsListView.setAdapter(contactsAdapter);
	}		
    
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        
        //Initialize the UI
        initUI();
           
        //fill Jade connection properties
        Properties jadeProperties = new Properties(); 
        jadeProperties.setProperty(Profile.MAIN_HOST, getString(R.string.jade_platform_host));
        jadeProperties.setProperty(Profile.MAIN_PORT, getString(R.string.jade_platform_port));
        //Get the phone number of my contact
        String telNum = ContactManager.getInstance().getMyContact().getNumTel();
        jadeProperties.setProperty(JICPProtocol.MSISDN_KEY, telNum);
        
        GeoNavigator.setLocationProvider("mygps");
     
        //try to get a JadeGateway
        try {
			JadeGateway.connect(MsnAgent.class.getName(), jadeProperties, this, this);
		} catch (Exception e) {
			//troubles during connection
			Toast.makeText(this, 
						   getString(R.string.error_msg_jadegw_connection), 
						   Integer.parseInt(getString(R.string.toast_duration))
						   ).show();
		}
    
    }


	public List<String> contactsToString(){

		List<String> strList = new ArrayList<String>();
		
		List<Contact> contactsList = ContactManager.getInstance().getOtherContactList();
		
 		for (Contact aid : contactsList){
			strList.add(aid.toString());
		}
 		
 		return strList;
	}
	
	
		
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
		
		GeoNavigator.stopLocationUpdate(this);
		
		if (gateway != null) {
			UnsubscribeCommand cmd = new UnsubscribeCommand();
			try {
				gateway.execute(cmd);
			} catch (StaleProxyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ControllerException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (!cmd.isSuccess()){
				Toast.makeText(this, 
						   cmd.getException().toString(), 
						   Integer.parseInt(getString(R.string.toast_duration))
						   ).show();
			}
			
			
			try {
				gateway.shutdownJADE();
			} catch (ConnectException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			gateway.disconnect(this);
		}
		
		
	}
	
	public void onConnected(JadeGateway arg0) {
		this.gateway = arg0;
	
		myLogger.log(Logger.INFO, "onConnected(): SUCCESS!");

		try {
				//FIXME: this code is needed to start JADE and to put online MyContact.
				//I cannot find another way to be sure that the agent is up!!!
				GetAIDCommandBehaviour getAIDBh = new GetAIDCommandBehaviour();
				gateway.execute(getAIDBh);
				
				//If agent is up
				if (getAIDBh.isSuccess()){
					//put my contact online
					ContactManager.getInstance().getMyContact().setOnline((AID) getAIDBh.getCommandResult());
					GeoNavigator.startLocationUpdate(this);        	
					TilabMsnApplication myApp =  (TilabMsnApplication) getApplication();
					gateway.execute(myApp.myBehaviour);
				} else {
					Toast.makeText(this, "Error during agent startup", 2000);
				}
			
				
						
		} catch(Exception e){
			Toast.makeText(this, e.toString(), 1000).show();
		}
	}



	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}

	
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENUITEM_ID_EXIT, R.string.menuitem_exit);
		return true;
	}
	
	public boolean onMenuItemSelected(int featureId, Item item) {
		super.onMenuItemSelected(featureId, item);
		
		switch(item.getId()) {
			case MENUITEM_ID_EXIT:
				finish();
			break;			
		}
		return true;
	}

	
	private void refreshContactList(){
		contactsAdapter.updateAdapter(ContactManager.getInstance().getMyContact().getLocation(), ContactManager.getInstance().getOtherContactList());
		contactsListView.setAdapter(contactsAdapter);
	}
	
	/**
	 * This class perform the GUI update
	 * @author s.semeria
	 *
	 */

	private class ContactListUpdater extends ContactsUIUpdater{

		public ContactListUpdater(Activity act) {
			super(act);
			// TODO Auto-generated constructor stub
		}

		@Override
		protected void handleUpdate() {
			// TODO Auto-generated method stub
		//	List<String> strList = contactsToString();
		//	ArrayAdapter<String> aa = new ArrayAdapter<String>(ContactListActivity.this,android.R.layout.simple_list_item_1,strList);
			
			contactsAdapter.updateAdapter(ContactManager.getInstance().getMyContact().getLocation(), ContactManager.getInstance().getOtherContactList());
			contactsListView.setAdapter(contactsAdapter);
		//	ListView lv = (ListView) activity.findViewById(R.id.contactsList);
		//	lv.setAdapter(aa);
		}
		
	}
	
	
	private class MapUpdater extends ContactsUIUpdater{

		public MapUpdater(Activity act) {
			super(act);
			// TODO Auto-generated constructor stub
		}

		protected void handleUpdate() {
			// TODO Auto-generated method stub
			MapView mapView = (MapView) activity.findViewById(R.id.myMapView);
			mapView.invalidate();
		}
	}
}