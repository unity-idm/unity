/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox.wizard;

import com.vaadin.server.BrowserWindowOpener;
import com.vaadin.server.Resource;
import com.vaadin.shared.Registration;
import com.vaadin.ui.Button;
import com.vaadin.ui.UI;

/**
 * This class extends {@link BrowserWindowOpener} with handy options.
 * Note that once popup window is opened, polling is enabled.
 * 
 * @author Roman Krysinski
 */
public class SandboxPopup extends BrowserWindowOpener 
{
	
	private static final int POLLING_INTERVAL = 1000;
	private Registration buttonClickReqistration;
	
	public SandboxPopup(Resource resource) 
	{
		super(resource);
		setFeatures("resizable,status=0,location=0");
		setWindowName("_blank");
	}
	
	public void attachButton(final Button button)
	{
		extend(button);
		button.addClickListener(event -> enablePolling());		
	}
	
	public void attachButtonOnce(final Button button)
	{
		extend(button);	
		buttonClickReqistration = button.addClickListener(event -> {
			buttonClickReqistration.remove();
			button.removeExtension(SandboxPopup.this);
			enablePolling();
		});		
	}
	
	private void enablePolling()
	{
		if (UI.getCurrent().getPollInterval() == -1) 
		{
			UI.getCurrent().setPollInterval(POLLING_INTERVAL);
		}
	}
}
