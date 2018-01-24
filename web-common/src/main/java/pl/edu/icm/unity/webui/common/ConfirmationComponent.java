/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common;

import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
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
	public ConfirmationComponent(Images icon, String title, String information)
	{
		initUI(icon, title, information);
	}

	public ConfirmationComponent(Images icon, String title)
	{
		initUI(icon, title, null);
	}
	
	private void initUI(Images icon, String title, String information)
	{
		VerticalLayout mainStatus = new VerticalLayout();
		mainStatus.setMargin(false);
		mainStatus.setSpacing(false);
		HorizontalLayout header = new HorizontalLayout();
		header.setSizeFull();
		header.setSpacing(false);
		header.setMargin(false);
		HorizontalLayout headerWrapper = new HorizontalLayout();
		headerWrapper.setSpacing(false);
		headerWrapper.setMargin(false);
		
		Label statusIcon = new Label(icon.getHtml());
		statusIcon.setContentMode(ContentMode.HTML);
		statusIcon.addStyleName(Styles.largeIcon.toString());
		
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
			mainStatus.setComponentAlignment(info, Alignment.MIDDLE_CENTER);
		}
		setCompositionRoot(mainStatus);
	}
}
