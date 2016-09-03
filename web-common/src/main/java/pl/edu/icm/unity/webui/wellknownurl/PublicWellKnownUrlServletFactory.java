/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServlet;
import pl.edu.icm.unity.webui.SimpleVaadinServletFactory;

/**
 * Contains confirmation vaadin servlet implementation
 * @author K. Benedyczak
 *
 */
@Component
public class PublicWellKnownUrlServletFactory extends SimpleVaadinServletFactory implements PublicWellKnownURLServlet
{
	@Autowired
	public PublicWellKnownUrlServletFactory(ApplicationContext applicationContext, UnityMessageSource msg, 
			UnityServerConfiguration config)
	{
		super(applicationContext, config, msg, PublicNavigationUI.class.getSimpleName(), new Properties(),
				UnityServerConfiguration.WELL_KNOWN_URL_THEME, 
				UnityServerConfiguration.WELL_KNOWN_URL_TEMPLATE,
				PublicWellKnownURLServlet.SERVLET_PATH);
	}
}
