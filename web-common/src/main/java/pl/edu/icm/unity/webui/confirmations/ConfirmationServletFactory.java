/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.SimpleVaadinServletFactory;

/**
 * Contains confirmation vaadin servlet implementation
 * @author P. Piernik
 *
 */
@Component
public class ConfirmationServletFactory extends SimpleVaadinServletFactory implements ConfirmationServlet
{
	@Autowired
	public ConfirmationServletFactory(ApplicationContext applicationContext, UnityMessageSource msg, 
			UnityServerConfiguration config)
	{
		super(applicationContext, config, msg, ConfirmationUI.class.getSimpleName(), new Properties(),
				UnityServerConfiguration.CONFIRMATION_THEME, 
				UnityServerConfiguration.CONFIRMATION_TEMPLATE, 
				ConfirmationServlet.SERVLET_PATH);
	}
}
