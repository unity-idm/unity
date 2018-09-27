/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin;

import com.vaadin.ui.Component;
import com.vaadin.ui.TabSheet;

import pl.edu.icm.unity.webui.ActivationListener;

/**
 * The main panel with tabs.
 * @author K. Benedyczak
 */
public class MainTabPanel extends TabSheet
{
	public MainTabPanel(Component... elements)
	{
		setSizeFull();
		
		for (Component element: elements)
			addTab(element);
		addSelectedTabChangeListener(tabChanged -> {
			Component selectedTab = getSelectedTab();
			if (selectedTab instanceof ActivationListener)
				((ActivationListener) selectedTab).stateChanged(true);
		});
	}
	
}
