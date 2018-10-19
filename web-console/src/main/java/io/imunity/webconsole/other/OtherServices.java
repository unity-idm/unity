/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole.other;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * Other service view
 * @author P.Piernik
 *
 */
public class OtherServices extends VerticalLayout implements View
{
	@Override
	public void enter(ViewChangeEvent event)
	{
		Label title = new Label();

		title.setCaption("Other services");
		title.setValue("Other services main");

		addComponent(title);
	}
}
