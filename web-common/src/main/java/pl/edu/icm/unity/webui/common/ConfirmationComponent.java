/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Image;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;


/**
 * Simple confirmation or information component. 
 * Shows icon and big information label next to it. Additionally can show smaller description below.
 * Contents is centered.
 * @author K. Benedyczak
 */
public class ConfirmationComponent extends CustomComponent
{
	public ConfirmationComponent(Resource icon, String title, String information)
	{
		initUI(icon, title, information);
	}

	public ConfirmationComponent(Resource icon, String title)
	{
		initUI(icon, title, null);
	}
	
	private void initUI(Resource icon, String title, String information)
	{
		VerticalLayout mainStatus = new VerticalLayout();
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		HorizontalLayout headerWrapper = new HorizontalLayout();
		Image statusIcon = new Image();
		statusIcon.setSource(icon);
		Label titleL = new Label(title);
		titleL.addStyleName(Styles.textXLarge.toString());
		headerWrapper.addComponents(statusIcon, titleL);
		headerWrapper.setComponentAlignment(statusIcon, Alignment.MIDDLE_CENTER);
		headerWrapper.setComponentAlignment(titleL, Alignment.MIDDLE_CENTER);
		header.addComponent(headerWrapper);
		header.setComponentAlignment(headerWrapper, Alignment.TOP_CENTER);
		mainStatus.addComponent(header);
		
		if (information != null)
		{
			Label info = new Label(information);
			info.addStyleName(Styles.textCenter.toString());
			info.addStyleName(Styles.textLarge.toString());
			mainStatus.addComponent(info);
		}
		setCompositionRoot(mainStatus);
	}
}
