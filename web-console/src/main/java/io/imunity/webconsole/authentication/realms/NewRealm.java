/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.authentication.realms;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

/**
 * View for add realm
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class NewRealm extends VerticalLayout implements View
{
	
	@Override
	public void enter(ViewChangeEvent event)
	{
		Label title = new Label();

		title.setCaption("New Realm");
		title.setValue("New Realm");
		setSizeFull();
		addComponent(title);
	}
}
