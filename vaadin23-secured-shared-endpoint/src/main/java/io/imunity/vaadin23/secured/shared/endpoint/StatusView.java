/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.secured.shared.endpoint;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import java.util.List;

@Route("/sec/status")
class StatusView extends Composite<VerticalLayout> implements BeforeEnterObserver
{

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		getContent().removeAll();
		String info = event.getLocation()
				.getQueryParameters()
				.getParameters()
				.getOrDefault("info", List.of())
				.stream().findFirst()
				.orElse("");
		VerticalLayout verticalLayout = new VerticalLayout(new H3(info));
		verticalLayout.setSizeFull();
		verticalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		getContent().add(verticalLayout);
	}
}
