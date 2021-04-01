/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido;

import io.imunity.fido.credential.FidoCredentialInfo;
import io.imunity.fido.service.FidoException;

import java.util.AbstractMap;

/**
 * Handles interaction between Credential Editor {@link io.imunity.fido.web.FidoCredentialEditor} and Credential Verificator {@link io.imunity.fido.service.FidoCredentialRegistrationVerificator}.
 * Performs init and validate Fido credential verification. Doesn't store credential in DB.
 *
 * @author R. Ledzinski
 */
public interface FidoRegistration
{
	/**
	 * Create registration request options that is passed to navigator.credentials.create() method on the client side.
	 *
	 * @param credentialName Name of credential the registration is for
	 * @param credentialConfiguration Credential configuration
	 * @param entityId user entity id (nullable)
	 * @param username Username property, existing username or new one. entityId take priority!
	 * @return Request ID and JSON registration options
	 * @throws FidoException in case JSON creation error
	 */
	AbstractMap.SimpleEntry<String, String> getRegistrationOptions(final String credentialName, final String credentialConfiguration,
																   final Long entityId, final String username,
																   final boolean useResidentKey) throws FidoException;

	/**
	 * Validates public key returned by navigator.credentials.create() method on the client side and store credentials.
	 *
	 * @param credentialName Name of credential the registration is for
	 * @param credentialConfiguration Credential configuration
	 * @param reqId        Registration request ID
	 * @param responseJson Authenticator response returned by navigator.credentials.create()
	 * @throws FidoException In case of any registration problems
	 */
	FidoCredentialInfo createFidoCredentials(final String credentialName, final String credentialConfiguration,
											 final String reqId, final String responseJson) throws FidoException;
}
