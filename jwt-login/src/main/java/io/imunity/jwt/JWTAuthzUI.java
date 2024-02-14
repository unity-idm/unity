/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.webui.UnityEndpointUIBase;

@Component("JWTAuthzUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class JWTAuthzUI extends UnityEndpointUIBase
{
	@Autowired
	public JWTAuthzUI(MessageSource msg)
	{
		super(msg, null);
	}

	@Override
	protected void enter(VaadinRequest request)
	{
		VerticalLayout contents = new VerticalLayout();
		contents.setMargin(false);
		contents.setSpacing(true);
		Label label = new Label("Authentication failure");
		contents.addComponent(label);
		setSizeFull();
		setContent(contents);
	}
}
