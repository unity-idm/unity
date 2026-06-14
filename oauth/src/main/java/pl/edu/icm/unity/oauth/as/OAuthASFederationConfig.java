/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import com.nimbusds.jose.jwk.JWKSet;

import eu.emi.security.authn.x509.X509CertChainValidator;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;

public record OAuthASFederationConfig(
		boolean membershipEnabled,
		String trustAnchorId,
		JWKSet trustAnchorJwks,
		X509CertChainValidator validator,
		ServerHostnameCheckingMode hostnameCheckingMode,
		String clientsGroup)
{
}
