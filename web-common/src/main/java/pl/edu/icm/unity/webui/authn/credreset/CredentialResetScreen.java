/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset;

import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.VerticalLayout;

/**
 * Minimal component covering full screen, and allowing for setting (and changing) the contents.
 * 
 * @author K. Benedyczak
 */
public class CredentialResetScreen extends CustomComponent
{
	public void setContents(Component contents)
	{
		VerticalLayout centerPositioning = new VerticalLayout();
		centerPositioning.setHeight(100, Unit.PERCENTAGE);
		centerPositioning.addComponent(contents);
		centerPositioning.setComponentAlignment(contents, Alignment.MIDDLE_CENTER);
		setSizeFull();
		setCompositionRoot(centerPositioning);
	}
}
