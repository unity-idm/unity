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
		super(applicationContext, config, ConfirmationUI.class.getSimpleName(), new Properties());
	}
}
