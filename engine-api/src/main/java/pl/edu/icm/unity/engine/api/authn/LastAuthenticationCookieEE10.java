/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;

import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class LastAuthenticationCookieEE10
{
	public static final String LAST_AUTHN_COOKIE = "lastAuthnUsed";
	private static final Duration MAX_AGE = Duration.ofDays(30);
	
	public static Optional<Cookie> createLastIdpCookie(String endpointPath, AuthenticationOptionKey idpKey)
	{
		if (endpointPath == null || idpKey == null)
			return Optional.empty();
		Cookie selectedIdp = new Cookie(LAST_AUTHN_COOKIE, idpKey.toStringEncodedKey());
		selectedIdp.setMaxAge((int) MAX_AGE.get(ChronoUnit.SECONDS));
		selectedIdp.setPath(endpointPath);
		selectedIdp.setHttpOnly(true);
		return Optional.of(selectedIdp);
	}
}
