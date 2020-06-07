/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.fido.credential;

import pl.edu.icm.unity.engine.api.authn.CredentialExchange;

/**
 * Allows for exchanging Fido credential.
 * TODO Detailed implementation
 *
 * @author R. Ledzinski
 */
public interface FidoExchange extends CredentialExchange
{
	static final String ID = "fido exchange";
}
