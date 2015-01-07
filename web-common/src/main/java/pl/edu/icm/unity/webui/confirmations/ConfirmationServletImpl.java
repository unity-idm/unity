/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.confirmations;

import java.util.Properties;

import javax.servlet.Servlet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationServlet;
import pl.edu.icm.unity.webui.VaadinEndpoint;
import pl.edu.icm.unity.webui.VaadinUIProvider;
import pl.edu.icm.unity.webui.WebSession;
import pl.edu.icm.unity.webui.bus.EventsBus;

import com.vaadin.server.DeploymentConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;

/**
 * Contains confirmation servlet implementation
 * @author P. Piernik
 *
 */
@Component
public class ConfirmationServletImpl implements ConfirmationServlet
{
	private ApplicationContext applicationContext;

	@Autowired
	public ConfirmationServletImpl(ApplicationContext applicationContext)
	{
		this.applicationContext = applicationContext;
	}
	
	@Override
	public Servlet getServiceServlet()
	{
		return new ConfirmationVaadinServlet();
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
							null, null, null);
					//uiProv.setCancelHandler(cancelHandler);
					event.getSession().addUIProvider(uiProv);
					DeploymentConfiguration depCfg = event.getService().getDeploymentConfiguration();
					Properties properties = depCfg.getInitParameters();
					String timeout = properties.getProperty(VaadinEndpoint.SESSION_TIMEOUT_PARAM);
					if (timeout != null)
						event.getSession().getSession().setMaxInactiveInterval(Integer.parseInt(timeout));

					if (WebSession.getCurrent() == null)
					{
						WebSession webSession = new WebSession(new EventsBus());
						WebSession.setCurrent(webSession);
					}			
				}
			});

			return service;
		}
	}

}
