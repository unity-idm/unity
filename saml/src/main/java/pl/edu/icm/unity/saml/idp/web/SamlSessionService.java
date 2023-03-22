/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.idp.web;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import pl.edu.icm.unity.saml.idp.ctx.SAMLAuthnContext;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Optional;

import static io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.noSignInContextException;

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

	public static SAMLAuthnContext getVaadinContext(VaadinContextSessionWithRequest session)
	{
		return LOGIN_IN_PROGRESS_SERVICE.getContext(session).orElseThrow(noSignInContextException());
	}

	public static void cleanContext(LoginInProgressService.SignInContextSession session)
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
	
	static class VaadinContextSessionWithRequest extends LoginInProgressService.VaadinContextSession
	{
		private final LoginInProgressService.SignInContextKey key;
		
		VaadinContextSessionWithRequest(VaadinSession session, VaadinRequest request)
		{
			super(session.getSession());
			this.key = get(request);
		}

		private static LoginInProgressService.SignInContextKey get(VaadinRequest request)
		{
			String key = request.getParameter(URL_PARAM_CONTEXT_KEY);
			if (key != null)
				return new LoginInProgressService.SignInContextKey(key);
			return LoginInProgressService.SignInContextKey.DEFAULT;
		}
		
		@Override
		public LoginInProgressService.SignInContextKey get()
		{
			return key;
		}
	}
}
