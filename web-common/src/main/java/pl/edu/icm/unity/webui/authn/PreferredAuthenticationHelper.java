/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import javax.servlet.http.Cookie;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

import pl.edu.icm.unity.webui.CookieHelper;

/**
 * Provides access to the last used IDP or the one requested with request parameter
 * @author K. Benedyczak
 */
public class PreferredAuthenticationHelper
{
	private static final String LAST_AUTHN_COOKIE = "lastAuthnUsed";
	/**
	 * Query param allowing for selecting IdP in request to the endpoint
	 */
	public static final String IDP_SELECT_PARAM = "uy_select_authn";
	
	public static String getPreferredIdp()
	{
		String requestedIdp = getIdpFromRequestParam(); 
		return requestedIdp == null ? getLastIdpFromCookie() : requestedIdp;
	}
	
	
	public static Cookie createLastIdpCookie(String endpointPath, String idpKey)
	{
		Cookie selectedIdp = new Cookie(LAST_AUTHN_COOKIE, idpKey);
		selectedIdp.setMaxAge(3600*24*30);
		selectedIdp.setPath(endpointPath);
		selectedIdp.setHttpOnly(true);
		return selectedIdp;
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
		return CookieHelper.getCookie(req.getCookies(), LAST_AUTHN_COOKIE);
	}
}
