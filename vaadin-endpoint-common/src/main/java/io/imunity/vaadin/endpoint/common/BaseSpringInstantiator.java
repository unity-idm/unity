/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.SpringInstantiator;
import org.springframework.context.ApplicationContext;

import java.util.stream.Stream;

class BaseSpringInstantiator extends SpringInstantiator
{
	BaseSpringInstantiator(VaadinService service, ApplicationContext context)
	{
		super(service, context);
	}

	@Override
	public Stream<VaadinServiceInitListener> getServiceInitListeners()
	{
		BaseVaadinServiceInitListener initializer = new BaseVaadinServiceInitListener();
		return Stream.concat(super.getServiceInitListeners(), Stream.of(initializer));
	}
}
