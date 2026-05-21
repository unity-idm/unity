/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.federation;

import eu.emi.security.authn.x509.X509Credential;

public record OAuthFederationEntityStatementConfig(
		String entityId,
		X509Credential federationCredential,
		X509Credential authenticationCredential,
		String callbackUrl,
		String superiorEntityId,
		long validitySeconds)
{
}
