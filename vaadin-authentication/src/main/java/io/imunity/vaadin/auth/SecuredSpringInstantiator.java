/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.SpringInstantiator;
import org.springframework.context.ApplicationContext;

import java.util.stream.Stream;

class SecuredSpringInstantiator extends SpringInstantiator
{
	private final String afterSuccessLoginRedirect;

	public SecuredSpringInstantiator(VaadinService service, ApplicationContext context)
	{
		super(service, context);
		this.afterSuccessLoginRedirect = null;

	}

	public SecuredSpringInstantiator(VaadinService service, ApplicationContext context, String afterSuccessLoginRedirect)
	{
		super(service, context);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
	}

	@Override
	public Stream<VaadinServiceInitListener> getServiceInitListeners() {
		ViewAccessCheckerInitializer initializer;
		if(afterSuccessLoginRedirect == null)
			initializer = new ViewAccessCheckerInitializer();
		else
			initializer = new ViewAccessCheckerInitializer(afterSuccessLoginRedirect);

		Stream<VaadinServiceInitListener> serviceInitListeners = super.getServiceInitListeners();
		return Stream.concat(serviceInitListeners, Stream.of(initializer));
	}
}
