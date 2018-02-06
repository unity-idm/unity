/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Button;
import com.vaadin.ui.Component;

/**
 * Button which allows to show or hide component(s). 
 * @author K. Benedyczak
 */
public class ExpandCollapseButton extends Button
{
	private boolean collapsed;
	private Component[] components;
	private ClickListener customListener;
	
	public ExpandCollapseButton(boolean initialCollapsed, Component... components)
	{
		collapsed = initialCollapsed;
		this.components = components;
		
		updateState();
		addStyleName(Styles.vButtonLink.toString());
		addClickListener(event -> {
			collapsed = !collapsed;
			updateState();
			if (customListener != null)
				customListener.buttonClick(event);
		});
	}
	
	public void setCustomListener(ClickListener customListener)
	{
		this.customListener = customListener;
	}

	private void updateState()
	{
		setIcon(collapsed ? Images.downArrow.getResource() 
				: Images.upArrow.getResource());
		for (Component c: components)
			c.setVisible(!collapsed);
	}
}
