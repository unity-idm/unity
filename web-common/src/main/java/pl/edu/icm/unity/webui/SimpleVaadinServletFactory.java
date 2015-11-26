/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.Properties;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

/**
 * Contains boilerplate for creation of a servlet exposing Vaadin UI by means of {@link VaadinUIProvider}
 * (which in turn takes care of IoC).
 * @author P. Piernik
 *
 */
public class SimpleVaadinServletFactory
{
	private ApplicationContext applicationContext;
	private UnityServerConfiguration config;
	private String uiClassName;
	private Properties configuration;

	public SimpleVaadinServletFactory(ApplicationContext applicationContext, UnityServerConfiguration config,
			String uiClassName, Properties configuration)
	{
		this.applicationContext = applicationContext;
		this.config = config;
		this.uiClassName = uiClassName;
		this.configuration = configuration;
	}
	
	public Servlet getServiceServlet()
	{
		return new SimpleVaadinServlet();
	}
	
	public Filter getServiceFilter()
	{
		return new InvocationContextSetupFilter(config, null, null);
	}

	private class SimpleVaadinServlet extends VaadinServlet
	{
				
		@Override
		protected VaadinServletService createServletService(DeploymentConfiguration deploymentConfiguration) 
				throws ServiceException 
		{	 
			final VaadinServletService service = super.createServletService(deploymentConfiguration);

			service.addSessionInitListener(new SessionInitListener()
			{
				@Override
				public void sessionInit(SessionInitEvent event) throws ServiceException
				{
					VaadinUIProvider uiProv = new VaadinUIProvider(applicationContext, 
							uiClassName,
							null, null, null, configuration, 
							VaadinEndpointProperties.THEME);
					event.getSession().addUIProvider(uiProv);
				}
			});
			return service;
		}
	}
}
