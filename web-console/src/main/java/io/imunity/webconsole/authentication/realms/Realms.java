/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.authentication.realms;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Lists all realms
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class Realms extends VerticalLayout implements View
{
	private RealmsManagement realmsMan;
	
	@Autowired
	public Realms(RealmsManagement realmsMan)
	{
		this.realmsMan = realmsMan;
	}
		
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
