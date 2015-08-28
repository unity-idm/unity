/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.wellknownurl;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.PublicWellKnownURLServlet;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.SimpleVaadinServletImpl;

/**
 * Contains confirmation vaadin servlet implementation
 * @author K. Benedyczak
 *
 */
@Component
public class PublicWellKnownUrlServletImpl extends SimpleVaadinServletImpl implements PublicWellKnownURLServlet
{
	@Autowired
	public PublicWellKnownUrlServletImpl(ApplicationContext applicationContext, UnityServerConfiguration config)
	{
		super(applicationContext, config, PublicNavigationUI.class.getSimpleName(), new Properties());
	}
}
