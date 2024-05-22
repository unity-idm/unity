/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;

class CustomErrorPageInitializer implements VaadinServiceInitListener, SessionInitListener
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, CustomErrorPageInitializer.class);

	private final MessageSource messageSource;

	CustomErrorPageInitializer(MessageSource messageSource)
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
			log.error("Vaadin initialization error:", errorEvent.getThrowable());
			errorEvent.getElement().ifPresent(el -> el.setText(messageSource.getMessage("Error")));
		});
	}
}
