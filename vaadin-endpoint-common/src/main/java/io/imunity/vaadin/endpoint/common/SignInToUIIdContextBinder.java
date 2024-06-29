/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.endpoint.common;

import static com.vaadin.flow.shared.ApplicationConstants.REQUEST_QUERY_PARAMETER;
import static io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.URL_PARAM_CONTEXT_KEY;

import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.UIInitEvent;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.WrappedSession;

import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.SignInContextKey;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.UrlParamSignInContextKey;
import io.imunity.vaadin.endpoint.common.consent_utils.LoginInProgressService.VaadinUIIdSignInContextKey;
import pl.edu.icm.unity.base.utils.Log;

public class SignInToUIIdContextBinder implements VaadinServiceInitListener, UIInitListener
{
	private static final Logger LOG = Log.getLogger(Log.U_SERVER_WEB, SignInToUIIdContextBinder.class);
	
	private final LoginInProgressContextMapper loginInProgressContextMapper;
	
	public SignInToUIIdContextBinder(LoginInProgressContextMapper loginInProgressContextMapper)
	{
		this.loginInProgressContextMapper = loginInProgressContextMapper;
	}

	@Override
	public void uiInit(UIInitEvent event)
	{
		SignInContextKey existingKey = getUrlParamSignInContextKey();
		WrappedSession wrappedSession = VaadinRequest.getCurrent().getWrappedSession();
		SignInContextKey newKey = new VaadinUIIdSignInContextKey(event.getUI().getUIId(), wrappedSession);
		loginInProgressContextMapper.putExistingContextUnderNewKey(wrappedSession, existingKey, newKey);
		LOG.debug("Additional context set existing:\"{}\" -> new:\"{}\"", existingKey, newKey);
	}
	
	public static SignInContextKey getUrlParamSignInContextKey()
	{
		Map<String, String[]> queryString = VaadinService.getCurrentRequest().getParameterMap();
		if (queryString.get(URL_PARAM_CONTEXT_KEY) != null && queryString.get(URL_PARAM_CONTEXT_KEY).length == 1)
		{
			return new UrlParamSignInContextKey(queryString.get(URL_PARAM_CONTEXT_KEY)[0]);
		}
		if (queryString.get(REQUEST_QUERY_PARAMETER) != null)
		{
			for (String query : queryString.get(REQUEST_QUERY_PARAMETER))
			{
				String[] param = query.split("=");
				if (URL_PARAM_CONTEXT_KEY.equals(param[0]))
				{
					return new UrlParamSignInContextKey(param[1]);
				}
			}
		}
		
		return UrlParamSignInContextKey.DEFAULT;
	}

	@Override
	public void serviceInit(ServiceInitEvent event)
	{
		event.getSource().addUIInitListener(this);
	}
	
	public interface LoginInProgressContextMapper
	{
		void putExistingContextUnderNewKey(WrappedSession wrappedSession,
				SignInContextKey existingKey,
				SignInContextKey newKey);
	}
}
