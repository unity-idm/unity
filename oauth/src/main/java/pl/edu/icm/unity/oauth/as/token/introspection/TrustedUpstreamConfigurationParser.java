/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;

class TrustedUpstreamConfigurationParser
{
	static List<TrustedUpstreamConfiguration> getConfig(OAuthASProperties oauthProperties)
	{
		Set<String> trustedUpstreamASKeys = oauthProperties
				.getStructuredListKeys(OAuthASProperties.TRUSTED_UPSTREAM_AS);
		List<TrustedUpstreamConfiguration> trustedUpstreamAS = new ArrayList<>();

		for (String trustedUpstreamKey : trustedUpstreamASKeys)
		{
			TrustedUpstreamConfiguration build = TrustedUpstreamConfiguration.builder()
					.withClientId(oauthProperties
							.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_ID))
					.withClientSecret(oauthProperties
							.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_SECRET))
					.withCertificate(oauthProperties
							.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CERTIFICATE))
					.withIntrospectionEndpointURL(oauthProperties.getValue(
							trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_INTROSPECTION_ENDPOINT_URL))
					.withIssuerURI(oauthProperties
							.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_ISSUER_URI))
					.withMetadataURL(oauthProperties
							.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_METADATA_URL))
					.withClientHostnameChecking(oauthProperties.getEnumValue(
							trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_HOSTNAME_CHECKING,
							ServerHostnameCheckingMode.class))
					.withClientTrustStore(oauthProperties
							.getValue(trustedUpstreamKey + OAuthASProperties.TRUSTED_UPSTREAM_AS_CLIENT_TRUSTSTORE))

					.build();

			trustedUpstreamAS.add(build);
		}
		return trustedUpstreamAS;
	}
}
