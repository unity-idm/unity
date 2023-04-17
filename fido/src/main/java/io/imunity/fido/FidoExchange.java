/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido;

import io.imunity.fido.service.FidoException;
import io.imunity.fido.web.v8.FidoRetrievalV8;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

import java.util.AbstractMap;

/**
 * Handles interaction between Credential Retrieval {@link FidoRetrievalV8} and Credential Verificator {@link io.imunity.fido.service.FidoCredentialVerificator}.
 * Performs authentication validation.
 *
 * @author R. Ledzinski
 */
public interface FidoExchange extends CredentialExchange
{
	String ID = "fido exchange";

	/**
	 * Create authentication request options that is passed to navigator.credentials.get() method on the client side.
	 *
	 * @param entityId user entity id (nullable)
	 * @param username Username property, existing username or new one. entityId take priority!
	 * @return Request ID and JSON authentication options
	 * @throws FidoException In case of problems with JSON parsing
	 */
	AbstractMap.SimpleEntry<String, String> getAuthenticationOptions(final Long entityId, final String username) throws FidoException;

	/**
	 * Validates signatures made by Authenticator.
	 *
	 * @param reqId    Authentication request ID
	 * @param jsonBody Authenticator response returned by navigator.credentials.get()
	 * @throws FidoException In case of any authentication problems
	 */
	AuthenticationResult verifyAuthentication(final String reqId, final String jsonBody) throws FidoException;
}
