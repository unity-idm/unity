/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.context.ApplicationContext;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

import io.imunity.vaadin.endpoint.common.BaseSpringInstantiator;
import io.imunity.vaadin.endpoint.common.SignInToUIIdContextBinder;
import io.imunity.vaadin.endpoint.common.SignInToUIIdContextBinder.LoginInProgressContextMapper;

class SecuredSpringInstantiator extends BaseSpringInstantiator
{
	private final String afterSuccessLoginRedirect;
	private final LoginInProgressContextMapper loginInProgressContextMapper;

	public SecuredSpringInstantiator(VaadinService service,
			ApplicationContext context,
			String afterSuccessLoginRedirect,
			LoginInProgressContextMapper loginInProgressContextMapper)
	{
		super(service, context);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
		this.loginInProgressContextMapper = loginInProgressContextMapper;
	}

	@Override
	public Stream<VaadinServiceInitListener> getServiceInitListeners()
	{
		NavigationAccessControlInitializer initializer = afterSuccessLoginRedirect == null
				? NavigationAccessControlInitializer.defaultInitializer()
				: NavigationAccessControlInitializer.withAfterSuccessLoginRedirect(afterSuccessLoginRedirect);
		
		SignInToUIIdContextBinder signInToUIIdBinder = loginInProgressContextMapper == null
				? null
				: new SignInToUIIdContextBinder(loginInProgressContextMapper);

		Stream<VaadinServiceInitListener> serviceInitListeners = super.getServiceInitListeners();
		return Stream.concat(
				serviceInitListeners, 
				Stream.of(initializer, signInToUIIdBinder).filter(Objects::nonNull)
		);
	}
}
