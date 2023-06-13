/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.servlet.Servlet;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.springframework.context.ApplicationContext;

import com.google.common.collect.Lists;
import com.vaadin.server.Constants;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.endpoint.ServletProvider;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

/**
 * Contains boilerplate for creation of a {@link UnityVaadinServlet}, with interface useful for simple 
 * pseudo-endpoints, which are not configured with the full power of Unity endpoints subsystem, but are accessible
 * from public & container-wide addresses.
 * @author P. Piernik
 * @author K. Benedyczak
 */
public class SimpleVaadinServletFactory implements ServletProvider
{
	private ApplicationContext applicationContext;
	protected final UnityServerConfiguration config;
	private String uiClassName;
	private Properties configuration;
	private MessageSource msg;
	private String themeConfigKey;
	private String servletPath;
	private String templateConfigKey;

	public SimpleVaadinServletFactory(ApplicationContext applicationContext, UnityServerConfiguration config,
			MessageSource msg, 
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
	
	@Override
	public ServletHolder getServiceServlet()
	{
		Servlet servlet = new UnityVaadinServlet(applicationContext, uiClassName, null, 
				null, null, 
				configuration, 
				getBootstrapHandlerGeneric(servletPath));

		ServletHolder holder = new ServletHolder(servlet);
		holder.setInitParameter(VaadinEndpoint.PRODUCTION_MODE_PARAM, "true");
		holder.setInitParameter(Constants.PARAMETER_WIDGETSET, "pl.edu.icm.unity.webui.customWidgetset");
		return holder;
	}
	
	@Override
	public List<FilterHolder> getServiceFilters()
	{
		return Lists.newArrayList(
				new FilterHolder(new InvocationContextSetupFilter(config, null, null, Collections.emptyList())));
	}

	private UnityBootstrapHandler getBootstrapHandlerGeneric(String uiPath)
	{
		String template = config.getValue(templateConfigKey);
		boolean debugMode = false;
		String theme = config.getConfiguredTheme(themeConfigKey, VaadinEndpoint.DEFAULT_THEME);
		String webContents = config.getValue(UnityServerConfiguration.DEFAULT_WEB_CONTENT_PATH);
		return new UnityBootstrapHandler(webContents, template, msg, theme, debugMode, 
				VaadinEndpoint.DEFAULT_HEARTBEAT, uiPath);
	}
}
