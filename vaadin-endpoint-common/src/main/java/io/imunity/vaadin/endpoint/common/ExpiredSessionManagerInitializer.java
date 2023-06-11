/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import com.vaadin.flow.server.*;
import io.imunity.vaadin.elements.VaadinInitParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;

import java.lang.invoke.MethodHandles;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Component
class ExpiredSessionManagerInitializer implements VaadinServiceInitListener, SessionInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final MessageSource messageSource;

	ExpiredSessionManagerInitializer(MessageSource messageSource)
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

		ofNullable(InvocationContext.getCurrent().getRealm())
				.map(AuthenticationRealm::getMaxInactivity)
				.or(() -> getMaxInactivityFormServletConfig(event))
				.ifPresent(wrappedSession::setMaxInactiveInterval);
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

	private static Optional<Integer> getMaxInactivityFormServletConfig(SessionInitEvent event)
	{
		return ofNullable(event.getService()
				.getDeploymentConfiguration()
				.getInitParameters()
				.getProperty(VaadinInitParameters.SESSION_TIMEOUT_PARAM)
		).map(Integer::parseInt);
	}
}
