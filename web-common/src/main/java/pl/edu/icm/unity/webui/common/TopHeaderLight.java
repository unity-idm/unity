/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

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
		addStyleName(Styles.verticalPadding10.toString());
		setMargin(true);
		setWidth(100, Unit.PERCENTAGE);
		setHeight(80, Unit.PIXELS);
		
		titleL = new Label(title);
		titleL.addStyleName(Styles.textHeading.toString());
		
		addComponent(titleL);
		setComponentAlignment(titleL, Alignment.MIDDLE_LEFT);
		titleL.setSizeUndefined();
	}
	
	public void setHeaderTitle(String title)
	{
		titleL.setValue(title);
	}
}
