/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import pl.edu.icm.unity.exceptions.FidoException;

import java.util.AbstractMap.SimpleEntry;

/**
 * FidoService exposing fido registration service to GUI.
 * Likely to be removed when Fido Authenticators are implemented.
 *
 * @author R. Ledzinski
 */
public interface FidoService
{
	/**
	 * Create registration request options that is passed to navigator.credentials.create() method on the client side.
	 *
	 * @param username Username property? Need to be clarified.... // TODO
	 * @return Request ID and JSON registration options
	 * @throws FidoException in case JSON creation error
	 */
	SimpleEntry<String, String> getRegistrationOptions(final String username) throws FidoException;

	/**
	 * Validates public key returned by navigator.credentials.create() method on the client side and store credentials.
	 *
	 * @param reqId Registration request ID
	 * @param responseJson Authenticator response returned by navigator.credentials.create()
	 * @throws FidoException In case of any registration problems
	 */
	void registerFidoCredentials(final String reqId, final String responseJson) throws FidoException;

	/**
	 * Create authentication request options that is passed to navigator.credentials.get() method on the client side.
	 *
	 * @param username Username property? Need to be clarified.... // TODO
	 * @return Request ID and JSON authentication options
	 * @throws FidoException In case of problems with JSON parsing
	 */
	SimpleEntry<String, String> getAuthenticationOptions(final String username) throws FidoException;

	/**
	 * Validates signatures made by Authenticator.
	 *
	 * @param reqId Authentication request ID
	 * @param jsonBody Authenticator response returned by navigator.credentials.get()
	 * @throws FidoException In case of any authentication problems
	 */
	void verifyAuthentication(final String reqId, final String jsonBody) throws FidoException;

	// FIXME to be removed
	void removeFidoCredentials();
}
