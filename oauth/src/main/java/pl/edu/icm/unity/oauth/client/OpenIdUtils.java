/*
 * Copyright (c) 2016 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client;

import java.util.HashMap;
import java.util.Map;

import com.nimbusds.jwt.JWTClaimsSet;

/**
 * General purpose utilities related to OpenID Connect
 * @author K. Benedyczak
 */
public class OpenIdUtils
{
	/**
	 * Converts {@link JWTClaimsSet} to plain attributes map
	 * @param claimSet
	 * @return
	 */
	public static Map<String, String> toAttributes(JWTClaimsSet claimSet)
	{
		Map<String, String> attributes = new HashMap<>();
		Map<String, Object> claims = claimSet.getClaims();
		for (Map.Entry<String, Object> claim: claims.entrySet())
		{
			if (claim.getValue() != null)
				attributes.put(claim.getKey(), claim.getValue().toString());
		}
		return attributes;
	}
}
