/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn;

import java.time.Duration;

import javax.servlet.http.Cookie;

public class LanguageCookie extends Cookie
{
	public static final String LANGUAGE_COOKIE = "language";
	public static final Duration LANGUAGE_COOKIE_MAX_AGE = Duration.ofSeconds(3600 * 24 * 31);

	public LanguageCookie(String lang)
	{
		super(LANGUAGE_COOKIE, lang);
		setPath("/");
		setMaxAge(Long.valueOf(LANGUAGE_COOKIE_MAX_AGE.toSeconds()).intValue());
		setHttpOnly(true);
	}

}
