/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinServlet;
import io.imunity.vaadin.endpoint.common.SpringVaadin2XServletService;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

public class SecuredSpringVaadin2XServletService extends SpringVaadin2XServletService
{
	private final String afterSuccessLoginRedirect;
	public SecuredSpringVaadin2XServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration, ApplicationContext applicationContext)
	{
		super(servlet, deploymentConfiguration, applicationContext);
		this.afterSuccessLoginRedirect = null;
	}

	public SecuredSpringVaadin2XServletService(VaadinServlet servlet, DeploymentConfiguration deploymentConfiguration,
	                                           ApplicationContext applicationContext, String afterSuccessLoginRedirect)
	{
		super(servlet, deploymentConfiguration, applicationContext);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
	}

	@Override
	protected Optional<Instantiator> loadInstantiators()
	{
		return Optional.of(new SecuredSpringInstantiator(this, context, afterSuccessLoginRedirect));
	}
}
