/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.idprovider.OAuth;
import io.imunity.webconsole.idprovider.SAML;

/**
 * Lists all flows
 * @author P.Piernik
 *
 */
public class Flows extends VerticalLayout implements View
{
	@Override
	public void enter(ViewChangeEvent event) {
		Label title = new Label();
		
		title.setCaption("Flows");
		title.setValue("Flows main");
		
	
		addComponent(title);
		
		Button link1 = new Button();
		link1.setCaption("Go to oauth");
		link1.addClickListener(e -> {
			getUI().getNavigator().navigateTo(OAuth.class.getSimpleName());
		});
		
		addComponent(link1);
		
		
		Button link2 = new Button();
		link2.setCaption("Go to oauth");
		link2.addClickListener(e -> {
			getUI().getNavigator().navigateTo(SAML.class.getSimpleName());
		});
		
		addComponent(link2);
		
	}
}
