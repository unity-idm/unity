/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.account_association_view;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H6;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import pl.edu.icm.unity.MessageSource;

import java.util.List;

@Route("/sec/status")
class StatusView extends Composite<VerticalLayout> implements BeforeEnterObserver, HasDynamicTitle
{
	final static String TITLE_PARAM = "title";
	final static String DESCRIPTION_PARAM = "description";
	private final MessageSource msg;

	StatusView(MessageSource msg)
	{
		this.msg = msg;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event)
	{
		getContent().removeAll();
		String title = getQueryParam(event, TITLE_PARAM);
		String description = getQueryParam(event, DESCRIPTION_PARAM);
		VerticalLayout verticalLayout = new VerticalLayout(new H3(title), new H6(description));
		verticalLayout.setSizeFull();
		verticalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
		getContent().add(verticalLayout);
	}

	private static String getQueryParam(BeforeEnterEvent event, String queryParamName)
	{
		return event.getLocation()
				.getQueryParameters()
				.getParameters()
				.getOrDefault(queryParamName, List.of())
				.stream().findFirst()
				.orElse("");
	}

	@Override
	public String getPageTitle()
	{
		return msg.getMessage("AssociationAccount.title");
	}
}
