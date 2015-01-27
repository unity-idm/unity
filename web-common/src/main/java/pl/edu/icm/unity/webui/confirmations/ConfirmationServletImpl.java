/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.webui.VaadinUIProvider;
import pl.edu.icm.unity.webui.authn.InvocationContextSetupFilter;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

/**
 * Contains confirmation vaadin servlet implementation
 * @author P. Piernik
 *
 */
@Component
public class ConfirmationServletImpl implements ConfirmationServlet
{
	private ApplicationContext applicationContext;
	private UnityServerConfiguration config;

	@Autowired
	public ConfirmationServletImpl(ApplicationContext applicationContext, UnityServerConfiguration config)
	{
		this.applicationContext = applicationContext;
		this.config = config;
	}
	
	@Override
	public Servlet getServiceServlet()
	{
		return new ConfirmationVaadinServlet();
	}
	
	@Override
	public Filter getServiceFilter()
	{
		return new InvocationContextSetupFilter(config, null, null);
	}

	private class ConfirmationVaadinServlet extends VaadinServlet
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
					VaadinUIProvider uiProv = new VaadinUIProvider(applicationContext, ConfirmationUI.class.getSimpleName(),
							null, null, null, null);
					event.getSession().addUIProvider(uiProv);
				}
			});
			return service;
		}
	}
}
