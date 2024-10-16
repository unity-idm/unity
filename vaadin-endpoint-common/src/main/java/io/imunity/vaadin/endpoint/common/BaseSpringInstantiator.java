/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import java.util.stream.Stream;

import org.springframework.beans.BeanInstantiationException;
import org.springframework.context.ApplicationContext;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.di.DefaultInstantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;

import pl.edu.icm.unity.base.message.MessageSource;

public class BaseSpringInstantiator extends DefaultInstantiator
{
	private final ApplicationContext context;

	public BaseSpringInstantiator(VaadinService service, ApplicationContext context)
	{
		super(service);
		this.context = context;
	}

	@Override
	public Stream<VaadinServiceInitListener> getServiceInitListeners()
	{
		return Stream.concat(super.getServiceInitListeners(), Stream.of(
				new RedirectUrlSessionStorageServiceInitListener(),
				new CustomErrorPageInitializer(context.getBean(MessageSource.class)),
				new CustomStylesInitializer(context.getBean(DefaultCssFileLoader.class),
						context.getBean(GlobalCustomCssFileLoader.class)))
		);
	}

	@Override
	public <T extends Component> T createComponent(Class<T> componentClass)
	{
		return context.getAutowireCapableBeanFactory().createBean(componentClass);
	}

	@Override
	public <T> T getOrCreate(Class<T> type)
	{
		if (context.getBeanNamesForType(type).length == 1)
		{
			return context.getBean(type);
		} else if (context.getBeanNamesForType(type).length > 1)
		{
			return createBean(type);
		} else
		{
			return context.getAutowireCapableBeanFactory().createBean(type);
		}
	}

	private <T> T createBean(Class<T> type)
	{
		try {
			return context.getAutowireCapableBeanFactory().createBean(type);
		} catch (BeanInstantiationException e) {
			throw new BeanInstantiationException(e.getBeanClass(), "Probably more than one suitable beans for in the context.", e);
		}
	}
}
