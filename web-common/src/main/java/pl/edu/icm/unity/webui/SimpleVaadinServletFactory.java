/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

/**
 * Contains boilerplate for creation of a {@link UnityVaadinServlet}, with interface useful for simple 
 * pseudo-endpoints, which are not configured with the full power of Unity endpoints subsystem, but are accessible
 * from public & container-wide addresses.
 * @author P. Piernik
 * @author K. Benedyczak
 */
public class SimpleVaadinServletFactory
{
	private ApplicationContext applicationContext;
	private UnityServerConfiguration config;
	private String uiClassName;
	private Properties configuration;
	private UnityMessageSource msg;
	private String themeConfigKey;
	private String servletPath;
	private String templateConfigKey;

	public SimpleVaadinServletFactory(ApplicationContext applicationContext, UnityServerConfiguration config,
			UnityMessageSource msg, 
			String uiClassName, Properties configuration,
			String themeConfigKey, 
			String templateConfigKey, 
			String servletPath)
	{
		this.applicationContext = applicationContext;
		this.config = config;
		this.msg = msg;
		this.uiClassName = uiClassName;
		this.themeConfigKey = themeConfigKey;
		this.templateConfigKey = templateConfigKey;
		this.servletPath = servletPath;
		this.configuration = configuration;
	}
	
	public Servlet getServiceServlet()
	{
		return new UnityVaadinServlet(applicationContext, uiClassName, null, 
				null, null, 
				configuration, 
				getBootstrapHandlerGeneric(servletPath));
	}
	
	public Filter getServiceFilter()
	{
		return new InvocationContextSetupFilter(config, null, null);
	}

	private UnityBootstrapHandler getBootstrapHandlerGeneric(String uiPath)
	{
		String template = config.getValue(templateConfigKey);
		boolean productionMode = true;
		String theme = config.getConfiguredTheme(themeConfigKey, VaadinEndpoint.DEFAULT_THEME);
		return new UnityBootstrapHandler(template, msg, theme, productionMode, 
				VaadinEndpoint.DEFAULT_HEARTBEAT, uiPath);
	}
}
