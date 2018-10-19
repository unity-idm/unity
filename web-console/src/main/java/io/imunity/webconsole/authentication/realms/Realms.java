/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.authentication.realms;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Lists all realms
 * @author P.Piernik
 *
 */
public class Realms extends VerticalLayout implements View
{
	@Override
	public void enter(ViewChangeEvent event) {
		Label title = new Label();
		
		title.setCaption("Realms");
		title.setValue("Realms main");
		
		
		
		
		VerticalLayout main = new VerticalLayout();
		main.setSizeFull();
		
		Button link1 = new Button();
		link1.setCaption("Go to new realm");
		link1.addClickListener(e -> {
			getUI().getNavigator().navigateTo(NewRealm.class.getSimpleName());
		});
		
		setSizeFull();
		main.addComponent(link1);
		
		addComponent(main);
	}
}
