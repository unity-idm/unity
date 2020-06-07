/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;
import pl.edu.icm.unity.webui.SimpleVaadinServletFactory;

/**
 * Shared Vaadin servlet providing access to various views, which are publicly available under well known links.
 */
@Component
public class PublicWellKnownUrlServletFactory extends SimpleVaadinServletFactory implements PublicWellKnownURLServletProvider
{
	@Autowired
	public PublicWellKnownUrlServletFactory(ApplicationContext applicationContext, MessageSource msg, 
			UnityServerConfiguration config)
	{
		super(applicationContext, config, msg, PublicNavigationUI.class.getSimpleName(), new Properties(),
				UnityServerConfiguration.WELL_KNOWN_URL_THEME, 
				UnityServerConfiguration.WELL_KNOWN_URL_TEMPLATE,
				PublicWellKnownURLServletProvider.SERVLET_PATH);
	}
}
