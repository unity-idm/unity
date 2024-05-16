/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.layout;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppVaadinProperties;

import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;

import io.imunity.vaadin.endpoint.common.Vaadin82XEndpointProperties;

@Tag("vaadin-authentication-layout")
public class AuthenticationLayout extends WrappedLayout
{
	@Autowired
	public AuthenticationLayout(ExtraPanelsConfiguration extraPanelsConfiguration)
	{
		super(extraPanelsConfiguration);
	}
	
	@Override
	protected void wrap(HasComponents main, Component toWrap,
			Vaadin82XEndpointProperties currentWebAppVaadinProperties, ExtraPanelsConfiguration config)
	{
		UnityLayoutWrapper.wrap(main, toWrap, getCurrentWebAppVaadinProperties(), config, true);
	}
}
