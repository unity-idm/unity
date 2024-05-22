/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import io.imunity.vaadin.endpoint.common.BaseSpringInstantiator;
import org.springframework.context.ApplicationContext;

import java.util.stream.Stream;

class SecuredSpringInstantiator extends BaseSpringInstantiator
{
	private final String afterSuccessLoginRedirect;

	public SecuredSpringInstantiator(VaadinService service, ApplicationContext context, String afterSuccessLoginRedirect)
	{
		super(service, context);
		this.afterSuccessLoginRedirect = afterSuccessLoginRedirect;
	}

	@Override
	public Stream<VaadinServiceInitListener> getServiceInitListeners()
	{
		NavigationAccessControlInitializer initializer;
		if(afterSuccessLoginRedirect == null)
			initializer = NavigationAccessControlInitializer.defaultInitializer();
		else
			initializer = NavigationAccessControlInitializer.withAfterSuccessLoginRedirect(afterSuccessLoginRedirect);

		Stream<VaadinServiceInitListener> serviceInitListeners = super.getServiceInitListeners();
		return Stream.concat(serviceInitListeners, Stream.of(initializer));
	}
}
