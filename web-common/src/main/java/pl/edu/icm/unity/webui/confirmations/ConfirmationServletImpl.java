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
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.SimpleVaadinServletImpl;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;

/**
 * Contains confirmation vaadin servlet implementation
 * @author P. Piernik
 *
 */
@Component
public class ConfirmationServletImpl extends SimpleVaadinServletImpl implements ConfirmationServlet
{
	@Autowired
	public ConfirmationServletImpl(ApplicationContext applicationContext, UnityServerConfiguration config)
	{
		super(applicationContext, config, ConfirmationUI.class.getSimpleName(), prepareConfig(config));
	}
	
	private static Properties prepareConfig(UnityServerConfiguration config)
	{
		Properties properties = new Properties();
		//a copy is set to endpoint's configuration so that the default is easily accessible
		if (config.isSet(UnityServerConfiguration.CONFIRMATION_THEME))
			properties.setProperty(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.DEF_THEME, 
				config.getValue(UnityServerConfiguration.CONFIRMATION_THEME));
		else if (config.isSet(UnityServerConfiguration.THEME))
			properties.setProperty(VaadinEndpointProperties.PREFIX + VaadinEndpointProperties.DEF_THEME, 
					config.getValue(UnityServerConfiguration.THEME));
		return properties;
	}
}
