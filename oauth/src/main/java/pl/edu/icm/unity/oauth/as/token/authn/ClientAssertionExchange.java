/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

public interface ClientAssertionExchange extends CredentialExchange
{
	String ID = "client assertion exchange";

	AuthenticationResult verifyClientAssertion(String clientAssertion, URI tokenEndpointUri);
}
