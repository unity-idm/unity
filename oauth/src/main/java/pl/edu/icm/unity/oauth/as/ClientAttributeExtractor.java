/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Map;

import pl.edu.icm.unity.base.attribute.AttributeExt;

class ClientAttributeExtractor
{
	static boolean isAllowedForRequestingWildcardScopes(Map<String, AttributeExt> attributes)
	{
		AttributeExt allowedForRequestingWildcardScopesA = attributes
				.get(OAuthSystemAttributesProvider.ALLOW_FOR_REQUESTING_WILDCARD_SCOPES);
		if (allowedForRequestingWildcardScopesA == null)
		{
			return false;
		} else
		{
			return allowedForRequestingWildcardScopesA.getValues()
					.stream()
					.anyMatch(v -> Boolean.parseBoolean(v.toString()));
		}
	}
}
