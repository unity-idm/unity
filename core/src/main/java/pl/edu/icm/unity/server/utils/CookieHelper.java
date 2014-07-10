/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.utils;

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
		for (Cookie cookie: cookies)
			if (name.equals(cookie.getName()))
				return cookie.getValue();
		return null;
	}
}
