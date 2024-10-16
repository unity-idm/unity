/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.ServiceException;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.VaadinServletService;
import org.springframework.context.ApplicationContext;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.engine.api.authn.DefaultUnsuccessfulAuthenticationCounter;
import pl.edu.icm.unity.engine.api.authn.NoOpLoginCounter;
import pl.edu.icm.unity.engine.api.authn.UnsuccessfulAccessCounter;

import java.util.Optional;

import static io.imunity.vaadin.endpoint.common.Vaadin2XWebAppContext.getCurrentWebAppResolvedEndpoint;

public class SpringVaadin2XServletService extends VaadinServletService
{
	private final VaadinServlet servlet;
	protected final ApplicationContext context;

	public SpringVaadin2XServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration, ApplicationContext applicationContext)
	{
		super(servlet, deploymentConfiguration);
		this.servlet = servlet;
		this.context = applicationContext;
	}

	@Override
	protected Optional<Instantiator> loadInstantiators()
	{
		return Optional.of(new BaseSpringInstantiator(this, context));
	}

	@Override
	public void init() throws ServiceException
	{
		super.init();
		context.getBeansOfType(UIInitListener.class)
				.values()
				.forEach(this::addUIInitListener);
		Object counter = servlet.getServletContext().getAttribute(UnsuccessfulAccessCounter.class.getName());
		if (counter == null)
		{
			ResolvedEndpoint description = getCurrentWebAppResolvedEndpoint();
			UnsuccessfulAccessCounter newCounter = description != null && description.getRealm() != null?
					new DefaultUnsuccessfulAuthenticationCounter(
							description.getRealm().getBlockAfterUnsuccessfulLogins(),
							description.getRealm().getBlockFor()* 1000L) :
					new NoOpLoginCounter();
			servlet.getServletContext().setAttribute(UnsuccessfulAccessCounter.class.getName(), newCounter);
		}
	}
}
