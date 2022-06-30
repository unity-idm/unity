/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.upman.av23.spring;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.*;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

public class SpringVaadin23ServletService extends VaadinServletService
{
	private final ApplicationContext context;

	public SpringVaadin23ServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration, ApplicationContext applicationContext) {
		super(servlet, deploymentConfiguration);
		this.context = applicationContext;
	}

	@Override
	protected Optional<Instantiator> loadInstantiators() throws ServiceException
	{
		Instantiator instantiator;
		try
		{
			instantiator = (Instantiator) Class.forName("com.vaadin.flow.spring.SpringInstantiator")
					.getConstructor(VaadinService.class, ApplicationContext.class)
					.newInstance(this, this.context);
		}
		catch (Exception e)
		{
			throw new ServiceException("Class com.vaadin.flow.spring.SpringInstantiator is not provided! See pom.xml maven-dependency-plugin");
		}
		return Optional.of(instantiator);
	}

	@Override
	public void init() throws ServiceException {
		super.init();
		context.getBeansOfType(UIInitListener.class)
				.values()
				.forEach(this::addUIInitListener);
	}
}
