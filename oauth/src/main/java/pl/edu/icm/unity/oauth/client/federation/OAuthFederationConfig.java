/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.net.URI;
import java.time.Duration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.config.OAuthClientConfiguration;

record OAuthFederationConfig(
		EntityID trustAnchorEntityId,
		URI trustAnchorListEndpoint,
		JWKSet trustAnchorJwks,
		Duration refreshInterval,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostnameCheckingMode)
{
	static OAuthFederationConfig from(OAuthClientConfiguration cfg) throws java.text.ParseException
	{
		EntityID trustAnchorId = new EntityID(cfg.federationTrustAnchorId);
		URI listEndpoint = URI.create(cfg.federationTrustAnchorId + "/list");
		JWKSet jwks = cfg.federationJwks != null
				? JWKSet.parse(cfg.federationJwks)
				: new JWKSet();
		Duration refresh = Duration.ofSeconds(cfg.federationMetadataValidity);
		return new OAuthFederationConfig(
				trustAnchorId,
				listEndpoint,
				jwks,
				refresh,
				cfg.federationValidator,
				cfg.federationHostnameCheckingMode != null
						? cfg.federationHostnameCheckingMode
						: ServerHostnameCheckingMode.FAIL);
	}
}
