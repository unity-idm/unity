/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import static io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.noSignInContextException;

import java.util.Optional;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.SignInContextSession;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.VaadinContextSession;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.SignInContextKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Methods responsible for SAML context handling
 * @author K. Benedyczak
 */
public class SamlSessionService
{
	/**
	 * Under this key the SAMLContext object is stored in the session.
	 */
	private static final String SESSION_SAML_CONTEXT = "samlAuthnContextKey";
	private static final LoginInProgressService<SAMLAuthnContext> LOGIN_IN_PROGRESS_SERVICE = new LoginInProgressService<>(
			SESSION_SAML_CONTEXT);

	public static final String URL_PARAM_CONTEXT_KEY = LoginInProgressService.URL_PARAM_CONTEXT_KEY;

	static void putExistingContextUnderNewKey(WrappedSession session, SignInContextKey existingKey, SignInContextKey newKey)
	{
		LOGIN_IN_PROGRESS_SERVICE.putExistingContextUnderNewKey(session, existingKey, newKey);
	}
	
	public static LoginInProgressService.SignInContextKey setContext(HttpSession session, SAMLAuthnContext context)
	{
		return LOGIN_IN_PROGRESS_SERVICE.setContext(session, context);
	}
	
	public static Optional<SAMLAuthnContext> getContext(HttpServletRequest req)
	{
		return LOGIN_IN_PROGRESS_SERVICE.getContext(req);
	}

	public static boolean hasVaadinContext()
	{
		return LOGIN_IN_PROGRESS_SERVICE.hasVaadinContext();
	}

	public static SAMLAuthnContext getVaadinContext()
	{
		return LOGIN_IN_PROGRESS_SERVICE.getVaadinContext();
	}

	public static SAMLAuthnContext getVaadinContext(VaadinContextSession session)
	{
		return LOGIN_IN_PROGRESS_SERVICE.getContext(session).orElseThrow(noSignInContextException());
	}

	public static void cleanContext(SignInContextSession session)
	{
		LOGIN_IN_PROGRESS_SERVICE.cleanUpSignInContextAttribute(session);
		session.removeAttribute(ResponseDocument.class.getName());
	}
	
	public static <T> void setAttribute(VaadinSession session, Class<T> clazz, T value)
	{ 
		session.getSession().setAttribute(clazz.getName(), value);
	}
	
	public static <T> T getAttribute(VaadinSession session, Class<T> clazz)
	{
		return clazz.cast(session.getSession().getAttribute(clazz.getName()));
	}
}
