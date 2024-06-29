/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import java.util.Optional;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.di.Instantiator;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.server.VaadinServlet;

import io.imunity.vaadin.endpoint.common.SignInToUIIdContextBinder.LoginInProgressContextMapper;
import io.imunity.vaadin.endpoint.common.SpringVaadin2XServletService;

public class SecuredSpringVaadin2XServletService extends SpringVaadin2XServletService
{
	private final String afterSuccessLoginRedirect;
	private final LoginInProgressContextMapper loginInProgressContextMapper;
	
	public SecuredSpringVaadin2XServletService(VaadinServlet servlet,
			DeploymentConfiguration deploymentConfiguration,
			ApplicationContext applicationContext)
	{
		this(servlet, deploymentConfiguration, applicationContext, null, null);
	}

	public SecuredSpringVaadin2XServletService(VaadinServlet servlet,
			DeploymentConfiguration deploymentConfiguration,
			ApplicationContext applicationContext,
			String afterSuccessLoginRedirect,
			LoginInProgressContextMapper loginInProgressContextMapper)
	{
		super(servlet, deploymentConfiguration, applicationContext);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
		this.loginInProgressContextMapper = loginInProgressContextMapper;
	}

	@Override
	protected Optional<Instantiator> loadInstantiators()
	{
		return Optional.of(new SecuredSpringInstantiator(this, context, afterSuccessLoginRedirect, loginInProgressContextMapper));
	}
}
