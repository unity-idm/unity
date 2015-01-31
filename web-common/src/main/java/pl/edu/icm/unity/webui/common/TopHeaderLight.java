/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Top bar with header. Only show informational text, and optional sub title.
 * @author K. Benedyczak
 */
public class TopHeaderLight extends HorizontalLayout
{
	private Label titleL;
	
	public TopHeaderLight(String title, UnityMessageSource msg)
	{
		setStyleName(Styles.header.toString());
		setMargin(true);
		setWidth(100, Unit.PERCENTAGE);
		setHeight(80, Unit.PIXELS);
		
		titleL = new Label(title);
		titleL.setStyleName(Styles.vLabelH1.toString());
		addComponent(titleL);
		setComponentAlignment(titleL, Alignment.MIDDLE_LEFT);
		titleL.setSizeUndefined();
	}
	
	public void setHeaderTitle(String title)
	{
		titleL.setValue(title);
	}
}
