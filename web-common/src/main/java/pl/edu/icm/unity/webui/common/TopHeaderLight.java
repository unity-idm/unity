/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.themes.Reindeer;
import com.vaadin.ui.themes.ValoTheme;

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
		setMargin(new MarginInfo(false, true, false, true));
		setWidth(100, Unit.PERCENTAGE);
		setHeight(80, Unit.PIXELS);
		
		titleL = new Label(title);
		titleL.addStyleName(ValoTheme.LABEL_H1);
		addComponent(titleL);
		setComponentAlignment(titleL, Alignment.MIDDLE_LEFT);
		titleL.setSizeUndefined();
	}
	
	public void setHeaderTitle(String title)
	{
		titleL.setValue(title);
	}
}
