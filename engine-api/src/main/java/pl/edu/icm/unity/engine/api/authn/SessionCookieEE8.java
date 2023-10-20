/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import javax.servlet.http.Cookie;

import pl.edu.icm.unity.engine.api.utils.CookieHelperEE8;

public class SessionCookieEE8
{
	private static final String UNITY_SESSION_COOKIE_PFX = "USESSIONID_";

	private final String name;
	private final String sessionId;
	
	public SessionCookieEE8(String realmName, String sessionId)
	{
		this.sessionId = sessionId;
		name = getSessionCookieName(realmName);
		
	}
	
	public Cookie toHttpCookie()
	{
		return CookieHelperEE8.setupHttpCookie(name, sessionId, -1);
	}
	
	public String getName()
	{
		return name;
	}
	
	public static String getSessionCookieName(String realmName)
	{
		return UNITY_SESSION_COOKIE_PFX + realmName;
	}
}
