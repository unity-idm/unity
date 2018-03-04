/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.confirmation.EmailConfirmationServletProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.webui.SimpleVaadinServletFactory;

/**
 * Contains confirmation vaadin servlet implementation
 * @author P. Piernik
 *
 */
@Component
public class EmailConfirmationServletFactory extends SimpleVaadinServletFactory implements EmailConfirmationServletProvider
{
	@Autowired
	public EmailConfirmationServletFactory(ApplicationContext applicationContext, UnityMessageSource msg, 
			UnityServerConfiguration config)
	{
		super(applicationContext, config, msg, EmailConfirmationUI.class.getSimpleName(), new Properties(),
				UnityServerConfiguration.CONFIRMATION_THEME, 
				UnityServerConfiguration.CONFIRMATION_TEMPLATE, 
				EmailConfirmationServletProvider.SERVLET_PATH);
	}
}
