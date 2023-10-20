/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import jakarta.servlet.http.Cookie;
import pl.edu.icm.unity.engine.api.utils.CookieHelper;

public class SessionCookie
{
	private static final String UNITY_SESSION_COOKIE_PFX = "USESSIONID_";

	private final String name;
	private final String sessionId;

	public SessionCookie(String realmName, String sessionId)
	{
		this.sessionId = sessionId;
		name = getSessionCookieName(realmName);
		
	}
	
	public Cookie toHttpCookie()
	{
		return CookieHelper.setupHttpCookie(name, sessionId, -1);
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
