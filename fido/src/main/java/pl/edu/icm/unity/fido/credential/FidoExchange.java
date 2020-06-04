/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.fido.credential;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;

/**
 * Allows for exchanging Fido credential.
 * TODO Detailed implementation
 *
 * @author R. Ledzinski
 */
public interface FidoExchange extends CredentialExchange
{
	static final String ID = "fido exchange";

	AuthenticationResult verify();
}
