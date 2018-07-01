/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

/**
 * Cookie finder
 * @author K. Benedyczak
 */
public class CookieHelper
{
	/**
	 * @param request
	 * @param name
	 * @return cookie name by value
	 */
	public static String getCookie(HttpServletRequest request, String name)
	{
		return getCookie(request.getCookies(), name);
	}

	/**
	 * @param request
	 * @param name
	 * @return cookie name by value
	 */
	public static String getCookie(Cookie[] cookies, String name)
	{
		if (cookies == null)
			return null;
		for (Cookie cookie: cookies)
			if (name.equals(cookie.getName()))
				return cookie.getValue();
		return null;
	}
	
	public static Cookie setupHttpCookie(String cookieName, String cookieValue, int maxAge)
	{
		Cookie httpCookie = new Cookie(cookieName, cookieValue);
		httpCookie.setPath("/");
		httpCookie.setSecure(true);
		httpCookie.setHttpOnly(true);
		httpCookie.setMaxAge(maxAge);
		return httpCookie;
	}
}
