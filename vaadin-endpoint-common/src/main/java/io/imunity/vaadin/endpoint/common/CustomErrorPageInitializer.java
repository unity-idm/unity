/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;

import pl.edu.icm.unity.base.message.MessageSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;

@Component
class CustomErrorPageInitializer implements VaadinServiceInitListener, SessionInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final MessageSource messageSource;

	public CustomErrorPageInitializer(MessageSource messageSource)
	{
		this.messageSource = messageSource;
	}

	@Override
	public void serviceInit(ServiceInitEvent event)
	{
		event.getSource().addSessionInitListener(this);
	}

	@Override
	public void sessionInit(SessionInitEvent event)
	{
		event.getSession().setErrorHandler(errorEvent ->
		{
			LOG.error("Vaadin initialization error:", errorEvent.getThrowable());
			UI.getCurrent().getElement().setText(messageSource.getMessage("Error"));
		});
	}
}
