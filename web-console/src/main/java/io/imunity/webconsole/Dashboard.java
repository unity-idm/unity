/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * Default page 
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class Dashboard extends VerticalLayout implements View
{
	@Override
	public void enter(ViewChangeEvent event) {
		Label title = new Label();
		
	//	title.setCaption("Dashboard");
		title.setValue("Dashboard main");
		
		addComponent(title);
	}
}
