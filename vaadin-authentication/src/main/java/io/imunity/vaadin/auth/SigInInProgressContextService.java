/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth;

import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.server.WrappedSession;
import org.apache.logging.log4j.Logger;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.SignInContextKey;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import pl.edu.icm.unity.engine.api.authn.RequestedAuthenticationContextClassReference;
import pl.edu.icm.unity.engine.api.authn.SigInInProgressContext;
import pl.edu.icm.unity.base.utils.Log;

@Component
public class SigInInProgressContextService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SigInInProgressContextService.class);
	
	private static final String SESSION_SIG_IN_IN_PROGRESS_CONTEXT = "sigInInProgressContext";
	private static final LoginInProgressService<SigInInProgressContext> LOGIN_IN_PROGRESS_SERVICE = new LoginInProgressService<>(
			SESSION_SIG_IN_IN_PROGRESS_CONTEXT);
	static final String URL_PARAM_CONTEXT_KEY = LoginInProgressService.URL_PARAM_CONTEXT_KEY;

	public static void putExistingContextUnderNewKey(WrappedSession session, SignInContextKey existingKey,
			SignInContextKey newKey)
	{
		LOGIN_IN_PROGRESS_SERVICE.putExistingContextUnderNewKey(session, existingKey, newKey);
	}

	public static LoginInProgressService.SignInContextKey setContext(HttpSession session,
			SigInInProgressContext context, SignInContextKey key)
	{
		return LOGIN_IN_PROGRESS_SERVICE.setContext(session, context, key);
	}

	public static SigInInProgressContext getContext(HttpServletRequest req)
	{	
		try
		{
			req.getSession();
		} catch (Exception e)
		{
			log.debug("Cannot get SigInInProgressContext", e);
			return new SigInInProgressContext(new RequestedAuthenticationContextClassReference(List.of(), List.of()));
		}
		
		return LOGIN_IN_PROGRESS_SERVICE.getContext(req)
				.orElse(new SigInInProgressContext(
						new RequestedAuthenticationContextClassReference(List.of(), List.of())));
	}

	public static SigInInProgressContext getVaadinContext()
	{
		try
		{
			return LOGIN_IN_PROGRESS_SERVICE.getVaadinContext();
		} catch (Exception e)
		{
			return new SigInInProgressContext(new RequestedAuthenticationContextClassReference(List.of(), List.of()));
		}
	}
}
