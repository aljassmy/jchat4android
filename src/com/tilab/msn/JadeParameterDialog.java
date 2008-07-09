package com.tilab.msn;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;


/**
 * Custom dialog employed to show to the user current host and port for JADE main container
 * making him able to change default settings (read from strings.xml) if he wishes to.
 * 
 * @author Cristina Cuc�
 * @author Marco Ughetti 
 * @author Stefano Semeria
 * @author Tiziana Trucco
 * @version 1.0 
 */
public class JadeParameterDialog extends Dialog {

	/** 
	 * The JADE main container host address. 
	 */
	private String jadeAddress;
	
	/**
	 * The JADE main container host port.
	 */
	private String jadePort;
	
	/** 
	 * GUI element containing the JADE address value
	 */
	private EditText jadeAddressEdt;
	
	/** 
	 * GUI element containing the JADE port value
	 */
	private EditText jadePortEdt;
	
	
	
	/**
	 * Instantiates a new jade parameter dialog.
	 * 
	 * @param context the current application context
	 */
	public JadeParameterDialog(Context context) {
		super(context);
		View v = initUI(context);
		this.setTitle(context.getString(R.string.label_params_title));
		this.setCancelable(false);
		this.setContentView(v);
		fillWithDefaults(context);
	}
	
	/**
	 * Retrieve default values for JADE host/port from strings.xml file
	 * 
	 * @param ctx the application context
	 */
	private void fillWithDefaults(Context ctx){
		jadeAddress = ctx.getString(R.string.jade_platform_host);
		jadePort = ctx.getString(R.string.jade_platform_port);
		jadeAddressEdt.setText(jadeAddress);
		jadePortEdt.setText(jadePort);
	}
	
	/**
	 * Gets the JADE main container host address.
	 * 
	 * @return the JADE main container host address.
	 */
	public String getJadeAddress(){
		return jadeAddress;
	}
	
	/**
	 * Gets the JADE main container host port.
	 * 
	 * @return the JADE main container host port
	 */
	public String getJadePort(){
		return jadePort;
	}
	
	
	/**
	 * Initializes the dialog UI, preparing the parent view containing view hierarchy 
	 * Layout is hardcoded here, no xml.
	 * 
	 * @param ctx the application context
	 * @return the parent view
	 */
	private View initUI(Context ctx){
		RelativeLayout layout = new RelativeLayout(ctx);
		layout.setPreferredHeight(LayoutParams.WRAP_CONTENT);
		layout.setPreferredWidth(LayoutParams.WRAP_CONTENT);
		
		TextView jadeAddress = new TextView(ctx);
		jadeAddress.setText("Jade platform address");
		jadeAddress.setId(1);
		layout.addView(jadeAddress,new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		
		jadeAddressEdt = new EditText(ctx);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.POSITION_BELOW, 1);
		jadeAddressEdt.setId(2);
		layout.addView(jadeAddressEdt,params);
		
		TextView jadePort = new TextView(ctx);
		jadePort.setText("Jade platform port");
		params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.POSITION_BELOW, 2);
		jadePort.setId(3);
		layout.addView(jadePort,params);
		
		jadePortEdt = new EditText(ctx);
		jadePortEdt.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.POSITION_BELOW, 3);
		jadePortEdt.setId(4);
		layout.addView(jadePortEdt,params);
		
				
		Button closeButton = new Button(ctx);
		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.CENTER_HORIZONTAL);
		params.addRule(RelativeLayout.POSITION_BELOW, 4);
		closeButton.setText("Close");
		closeButton.setOnClickListener(new View.OnClickListener(){
			/**
			 * Handles clicking on close button 
			 */
			public void onClick(View arg0) {
					String tmpVar = JadeParameterDialog.this.jadeAddressEdt.getText().toString();
					if (tmpVar.length() > 0){
						JadeParameterDialog.this.jadeAddress = tmpVar;
					}
					
					tmpVar = JadeParameterDialog.this.jadePortEdt.getText().toString();
					if (tmpVar.length() > 0){
						JadeParameterDialog.this.jadePort = tmpVar;
					}
					
					JadeParameterDialog.this.dismiss();
			}
			
		});
		
		layout.addView(closeButton,params);
		
		return layout;
	}
	
	
}
