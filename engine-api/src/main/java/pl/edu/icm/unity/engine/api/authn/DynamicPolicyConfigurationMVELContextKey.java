/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum DynamicPolicyConfigurationMVELContextKey
{
	
	userOptIn(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "userOptIn"),
	hasValid2FCredential(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "hasValid2FCredential"),
	authentication1F(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "authentication1F"),
	attr(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "attr"),
	attrObj(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "attrObj"),
	idsByType(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "idsByType"),
	groups(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "groups"),
	upstreamACRs(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "upstreamACRs"),
	upstreamIdP(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "upstreamIdP"),
	upstreamProtocol(DynamicPolicyConfigurationMVELContextKey.descriptionPrefix + "upstreamProtocol");

	public static final String descriptionPrefix = "DynamicPolicyConfigurationMVELContextKey.";
	public final String descriptionKey;

	private DynamicPolicyConfigurationMVELContextKey(String descriptionKey)
		{
			this.descriptionKey = descriptionKey;
		}

	public static Map<String, String> toMap()
	{
		return Stream.of(values())
				.collect(Collectors.toMap(v -> v.name(), v -> v.descriptionKey));
	}
}
