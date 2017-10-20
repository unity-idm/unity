/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.ui.Alignment;
import com.vaadin.v7.ui.HorizontalLayout;
import com.vaadin.v7.ui.Label;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;

/**
 * Top bar with header. Only show informational text, and optional sub title.
 * @author K. Benedyczak
 */
public class TopHeaderLight extends HorizontalLayout
{
	private Label titleL;
	
	public TopHeaderLight(String title, UnityMessageSource msg)
	{
		addStyleName(Styles.header.toString());
		setMargin(true);
		setWidth(100, Unit.PERCENTAGE);
		
		titleL = new Label(title);
		titleL.addStyleName(Styles.textEndpointName.toString());
		
		addComponent(titleL);
		setComponentAlignment(titleL, Alignment.MIDDLE_LEFT);
		titleL.setSizeUndefined();
	}
	
	public void setHeaderTitle(String title)
	{
		titleL.setValue(title);
	}
}
