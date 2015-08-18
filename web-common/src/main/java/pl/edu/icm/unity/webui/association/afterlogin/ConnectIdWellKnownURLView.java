/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.association.afterlogin;

import org.vaadin.teemu.wizards.Wizard;

import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;


/**
 * A view that can be used under a well-known URL to trigger account association.
 * @author K. Benedyczak
 */
public class ConnectIdWellKnownURLView extends CustomComponent implements View
{
	public ConnectIdWellKnownURLView(ConnectIdWizardProvider connectIdProvider)
	{
		Wizard wizardInstance = connectIdProvider.getWizardInstance();
		String caption = connectIdProvider.getCaption();
		
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(true);
		main.setMargin(true);
		
		Label title = new Label(caption);
		title.addStyleName(Styles.textLarge.toString());
		main.addComponent(title);
		main.addComponent(wizardInstance);

		setCompositionRoot(main);
	}
	
	@Override
	public void enter(ViewChangeEvent event)
	{
	}
}
