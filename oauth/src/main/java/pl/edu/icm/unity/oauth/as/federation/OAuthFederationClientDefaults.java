/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.federation;

import java.util.List;

import pl.edu.icm.unity.oauth.as.OAuthASProperties;

public record OAuthFederationClientDefaults(boolean allowAnyScopes, List<String> allowedScopes)
{
	public OAuthFederationClientDefaults
	{
		allowedScopes = allowedScopes != null ? List.copyOf(allowedScopes) : List.of();
	}

	public static OAuthFederationClientDefaults from(OAuthASProperties props)
	{
		return new OAuthFederationClientDefaults(
				props.getBooleanValue(OAuthASProperties.FEDERATION_ALLOW_ANY_SCOPES),
				props.getListOfValues(OAuthASProperties.FEDERATION_ALLOWED_SCOPES));
	}
}
