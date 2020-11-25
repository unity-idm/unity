/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

/**
 * Button which allows to show or hide component(s). 
 * @author K. Benedyczak
 */
public class ExpandCollapseButton extends CustomComponent
{
	private boolean collapsed;
	private Component[] components;
	private ClickListener customListener;
	private Button button;
	
	public ExpandCollapseButton(String message, boolean initialCollapsed, Component... components)
	{
		collapsed = initialCollapsed;
		this.components = components;

		Label info = new Label(message);
		button = new Button();
		button.addStyleName(Styles.vButtonLink.toString());
		button.addClickListener(event -> {
			collapsed = !collapsed;
			updateState();
			if (customListener != null)
				customListener.buttonClick(event);
		});
		
		HorizontalLayout hl = new HorizontalLayout();
		hl.setMargin(false);
		hl.addComponents(info, button);
		setCompositionRoot(hl);
		
		updateState();
	}
	
	public void setCustomListener(ClickListener customListener)
	{
		this.customListener = customListener;
	}

	private void updateState()
	{
		button.setIcon(collapsed ? Images.downArrow.getResource() 
				: Images.upArrow.getResource());
		for (Component c: components)
			c.setVisible(!collapsed);
	}
}
