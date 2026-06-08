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
import pl.edu.icm.unity.oauth.client.config.FederationConfig;

record OAuthFederationConfig(
		EntityID trustAnchorEntityId,
		URI trustAnchorListEndpoint,
		JWKSet trustAnchorJwks,
		Duration refreshInterval,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostnameCheckingMode)
{
	static OAuthFederationConfig from(FederationConfig cfg) throws java.text.ParseException
	{
		EntityID trustAnchorId = new EntityID(cfg.trustAnchorId);
		URI listEndpoint = URI.create(cfg.trustAnchorId + "/list");
		JWKSet jwks = cfg.jwks != null
				? JWKSet.parse(cfg.jwks)
				: new JWKSet();
		Duration refresh = Duration.ofSeconds(cfg.metadataValidity);
		return new OAuthFederationConfig(
				trustAnchorId,
				listEndpoint,
				jwks,
				refresh,
				cfg.validator,
				cfg.hostnameCheckingMode != null
						? cfg.hostnameCheckingMode
						: ServerHostnameCheckingMode.FAIL);
	}
}
