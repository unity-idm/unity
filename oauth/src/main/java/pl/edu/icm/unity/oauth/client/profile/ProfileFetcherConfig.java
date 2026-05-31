/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.profile;

import com.nimbusds.oauth2.sdk.http.HTTPRequest.Method;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientAuthnMode;
import pl.edu.icm.unity.oauth.client.config.OAuthProviderConfiguration;

public record ProfileFetcherConfig(
		String truststoreName,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostNameCheckingMode,
		ClientAuthnMode clientAuthnModeForProfileAccess,
		Method clientHttpMethodForProfileAccess,
		String clientId,
		String clientSecret,
		String accessTokenEndpoint)
{
	public static ProfileFetcherConfig from(OAuthProviderConfiguration providerCfg)
	{
		return new ProfileFetcherConfig(
				providerCfg.truststoreName,
				providerCfg.validator,
				providerCfg.hostNameCheckingMode,
				providerCfg.clientAuthnModeForProfileAccess,
				providerCfg.clientHttpMethodForProfileAccess,
				providerCfg.clientId,
				providerCfg.clientSecret,
				providerCfg.accessTokenEndpoint);
	}
}
