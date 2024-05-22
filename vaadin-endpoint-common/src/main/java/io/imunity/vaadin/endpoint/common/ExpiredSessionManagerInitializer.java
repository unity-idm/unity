/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common;

import static java.util.Optional.ofNullable;

import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.vaadin.flow.server.CustomizedSystemMessages;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.SessionInitEvent;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.server.WrappedSession;

import io.imunity.vaadin.elements.VaadinInitParameters;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;

@Component
class ExpiredSessionManagerInitializer implements VaadinServiceInitListener, SessionInitListener
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, ExpiredSessionManagerInitializer.class);

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
		log.debug("Session {} created, max inactivity set to {}",
				wrappedSession.getId(),
				wrappedSession.getMaxInactiveInterval());

		VaadinService.getCurrent().setSystemMessagesProvider(systemMessagesInfo ->
		{
			CustomizedSystemMessages messages = new CustomizedSystemMessages();
			messages.setSessionExpiredCaption(messageSource.getMessage("SessionExpiration.expired"));
			messages.setSessionExpiredMessage(messageSource.getMessage("SessionExpiration.refresh"));
			messages.setSessionExpiredNotificationEnabled(true);
			messages.setSessionExpiredURL(
					VaadinServlet.getCurrent().getServletContext().getContextPath() +
					VaadinServlet.getFrontendMapping().replace("*", "")
			);

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
