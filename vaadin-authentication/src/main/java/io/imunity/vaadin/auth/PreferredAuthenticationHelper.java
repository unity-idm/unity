/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import java.util.function.Consumer;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;

import io.imunity.vaadin.endpoint.common.SessionStorage;
import pl.edu.icm.unity.engine.api.authn.LastAuthenticationCookie;
import pl.edu.icm.unity.engine.api.utils.CookieHelper;

/**
 * Provides access to the last used IDP or the one requested with request parameter
 */
public class PreferredAuthenticationHelper
{
	/**
	 * Query param allowing for selecting IdP in request to the endpoint
	 */
	public static final String IDP_SELECT_PARAM = "uy_select_authn";
	
	public static void consumePreferredAuthn(Consumer<String> preferedConsumer)
	{
		SessionStorage.consumeSelectedAuthn((storedValue, currentRelativeURI) ->
		{
			String requestedIdp = getIdpFromRequestParam(); 
			if (requestedIdp == null && storedValue != null)
			{
				requestedIdp = storedValue;
			}
			preferedConsumer.accept(requestedIdp == null ? getLastIdpFromCookie() : requestedIdp);		
		});
	}
	
	private static String getIdpFromRequestParam()
	{
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		return req.getParameter(IDP_SELECT_PARAM);
	}
	
	private static String getLastIdpFromCookie()
	{
		VaadinRequest req = VaadinService.getCurrentRequest();
		if (req == null)
			return null;
		return CookieHelper.getCookie(req.getCookies(), LastAuthenticationCookie.LAST_AUTHN_COOKIE);
	}
}
