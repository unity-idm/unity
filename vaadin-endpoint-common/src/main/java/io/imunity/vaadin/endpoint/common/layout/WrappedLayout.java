/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppVaadinProperties;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.router.RouterLayout;

import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

@Tag("vaadin-wrapped-layout")
public class WrappedLayout extends Component implements RouterLayout, HasComponents
{
	private UnityServerConfiguration config;
	
	@Autowired
	public WrappedLayout(UnityServerConfiguration unityServerConfiguration)
	{
		this.config = unityServerConfiguration;
	}
	
	@Override
	public void showRouterLayoutContent(HasElement content)
	{
		FlexLayout wrapped = new FlexLayout();
		wrapped.setSizeFull();
		wrapped.getStyle().set("display", "");
		Div contentDiv = new Div();
		contentDiv.getElement()
				.appendChild(content.getElement());
		wrapped.setJustifyContentMode(JustifyContentMode.CENTER);
		wrapped.add(contentDiv);
		wrap(this, wrapped, getCurrentWebAppVaadinProperties(), config);
	}

	
	protected void wrap(HasComponents main, Component toWrap,
			Vaadin82XEndpointProperties currentWebAppVaadinProperties, UnityServerConfiguration config)
	{
		UnityLayoutWrapper.wrap(main, toWrap, getCurrentWebAppVaadinProperties(), config, false);
	}
	
}
