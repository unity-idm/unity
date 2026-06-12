/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import java.time.Duration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.openid.connect.sdk.federation.entities.EntityID;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import pl.edu.icm.unity.oauth.client.config.FederationConfig;

record OAuthFederationConfig(
		EntityID trustAnchorEntityId,
		JWKSet trustAnchorJwks,
		Duration refreshInterval,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostnameCheckingMode)
{
	static OAuthFederationConfig from(FederationConfig cfg) throws java.text.ParseException
	{
		if (cfg.jwks == null)
			throw new java.text.ParseException(
					"Trust anchor JWKS must be configured for federation with " + cfg.trustAnchorId, 0);
		EntityID trustAnchorId = new EntityID(cfg.trustAnchorId);
		JWKSet jwks = JWKSet.parse(cfg.jwks);
		Duration refresh = Duration.ofSeconds(cfg.metadataValidity);
		return new OAuthFederationConfig(
				trustAnchorId,
				jwks,
				refresh,
				cfg.validator,
				cfg.hostnameCheckingMode != null
						? cfg.hostnameCheckingMode
						: ServerHostnameCheckingMode.FAIL);
	}
}
