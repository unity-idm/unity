/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.Optional;

import javax.servlet.http.Cookie;

import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;

public class LastAuthenticationCookie
{
	public static final String LAST_AUTHN_COOKIE = "lastAuthnUsed";
	
	public static Optional<Cookie> createLastIdpCookie(String endpointPath, AuthenticationOptionKey idpKey)
	{
		if (endpointPath == null || idpKey == null)
			return Optional.empty();
		Cookie selectedIdp = new Cookie(LAST_AUTHN_COOKIE, idpKey.toStringEncodedKey());
		selectedIdp.setMaxAge(3600*24*30);
		selectedIdp.setPath(endpointPath);
		selectedIdp.setHttpOnly(true);
		return Optional.of(selectedIdp);
	}
}
