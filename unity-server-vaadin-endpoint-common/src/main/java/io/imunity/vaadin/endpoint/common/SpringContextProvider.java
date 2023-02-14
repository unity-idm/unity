/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringContextProvider implements ApplicationContextAware
{
	private static ApplicationContext context;

	public static ApplicationContext getContext()
	{
		return context;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException
	{
		SpringContextProvider.context = context;
	}
}
