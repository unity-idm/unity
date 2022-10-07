/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.server.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;

import java.lang.invoke.MethodHandles;

@Component
public class ExpiredSessionManagerInitializer implements VaadinServiceInitListener, SessionInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final MessageSource messageSource;

	public ExpiredSessionManagerInitializer(MessageSource messageSource)
	{
		this.messageSource = messageSource;
	}

	@Override
	public void serviceInit(ServiceInitEvent event)
	{
		event.getSource().addSessionInitListener(this);
	}

	@Override
	public void sessionInit(SessionInitEvent event) {
		WrappedSession wrappedSession = event.getSession().getSession();
		wrappedSession.setMaxInactiveInterval(InvocationContext.getCurrent().getRealm().getMaxInactivity());
		LOG.debug("Session {} created, max inactivity set to {}",
				wrappedSession.getId(),
				wrappedSession.getMaxInactiveInterval());

		VaadinService.getCurrent().setSystemMessagesProvider(systemMessagesInfo ->
		{
			CustomizedSystemMessages messages = new CustomizedSystemMessages();
			messages.setSessionExpiredCaption(messageSource.getMessage("SessionExpiration.expired"));
			messages.setSessionExpiredMessage(messageSource.getMessage("SessionExpiration.refresh"));
			messages.setSessionExpiredNotificationEnabled(true);
			messages.setSessionExpiredURL(VaadinServlet.getFrontendMapping().replace("*", ""));
			return messages;
		});
	}
}
