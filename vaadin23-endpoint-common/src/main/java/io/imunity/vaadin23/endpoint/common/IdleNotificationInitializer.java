/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin23.endpoint.common;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.*;
import io.imunity.vaadin23.elements.UnityIdleNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import static io.imunity.vaadin23.endpoint.common.LogoutView.LOGOUT_URL;
import static io.imunity.vaadin23.endpoint.common.Vaadin23WebAppContext.getCurrentWebAppVaadinProperties;

@Component
public class IdleNotificationInitializer implements VaadinServiceInitListener, SessionInitListener, SessionDestroyListener, UIInitListener
{
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	private final Vaddin23WebLogoutHandler vaddin23WebLogoutHandler;
	private final MessageSource messageSource;

	public IdleNotificationInitializer(Vaddin23WebLogoutHandler vaddin23WebLogoutHandler, MessageSource messageSource)
	{
		this.vaddin23WebLogoutHandler = vaddin23WebLogoutHandler;
		this.messageSource = messageSource;
	}

	@Override
	public void serviceInit(ServiceInitEvent event)
	{
		event.getSource().addSessionInitListener(this);
		event.getSource().addSessionDestroyListener(this);
		event.getSource().addSessionInitListener(this);
	}

	@Override
	public void sessionDestroy(SessionDestroyEvent event) {
		VaadinServletRequest request = (VaadinServletRequest) VaadinRequest.getCurrent();
		if (request != null) {
			WrappedSession wrappedSession = request.getWrappedSession(false);
			if (wrappedSession != null) {
				LOG.debug("Triggering spring logout from Vaadin session destroy: {}",
						wrappedSession.getId());
				vaddin23WebLogoutHandler.logout();
			}
		}
	}

	@Override
	public void sessionInit(SessionInitEvent event) {
		WrappedSession wrappedSession = event.getSession().getSession();
		wrappedSession.setMaxInactiveInterval(InvocationContext.getCurrent().getRealm().getMaxInactivity());
		LOG.debug("Session {} created, max inactivity set to {}",
				wrappedSession.getId(),
				wrappedSession.getMaxInactiveInterval());
	}

	@Override
	public void uiInit(UIInitEvent initEvent)
	{
		int secondsBeforeShowingSessionExpirationWarning = getCurrentWebAppVaadinProperties().getSecondsBeforeShowingSessionExpirationWarning();
		LOG.debug("A new UI has been initialized, warning will be shown {}s before expiration", secondsBeforeShowingSessionExpirationWarning);
		UnityIdleNotification idleNotification = new UnityIdleNotification(secondsBeforeShowingSessionExpirationWarning);
		String warning = messageSource.getMessage("SessionExpiration.expirationWarning",
				secondsBeforeShowingSessionExpirationWarning);
		idleNotification.setMessage(warning);
		idleNotification.addExtendSessionButton(messageSource.getMessage("SessionExpiration.extend"));
		idleNotification.addRedirectButton(messageSource.getMessage("SessionExpiration.logoutNow"),
				LOGOUT_URL);
		idleNotification.setRedirectAtTimeoutUrl(LOGOUT_URL);
		idleNotification.addCloseButton();
		idleNotification.setCloseNotificationOnOutsideClick(true);
		idleNotification.setExtendSessionOnOutsideClick(false);
		initEvent.getUI().add(idleNotification);

		WrappedSession wrappedSession = VaadinSession.getCurrent().getSession();
		String sessionId = wrappedSession.getId();
		initEvent.getUI().addDetachListener(event -> LOG.debug("Closing UI of session {}", sessionId));

		UIInSessionHolder.addUIToSession(initEvent.getUI(), (WrappedHttpSession) wrappedSession);
		LOG.debug("Saved UI in session {}", sessionId);
	}

	public static class UIInSessionHolder
	{
		private static final String SESSION_ATTR = UIInSessionHolder.class.getCanonicalName();

		public static void addUIToSession(UI ui, WrappedHttpSession session) {
			synchronized(session.getHttpSession()) {
				@SuppressWarnings("unchecked")
				List<UI> uiList = (List<UI>) session.getAttribute(SESSION_ATTR);
				if (uiList == null) {
					uiList = new ArrayList<>();
					session.setAttribute(SESSION_ATTR, uiList);
				}
				uiList.add(ui);
			}
		}
	}
}
