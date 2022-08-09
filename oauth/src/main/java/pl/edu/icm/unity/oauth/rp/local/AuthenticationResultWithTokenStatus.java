/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;

class AuthenticationResultWithTokenStatus
{
	final AuthenticationResult result;
	final Optional<TokenStatus> token;

	AuthenticationResultWithTokenStatus(AuthenticationResult result)
	{
		this(result, null);
	}

	AuthenticationResultWithTokenStatus(AuthenticationResult result, TokenStatus token)
	{
		this.result = result;
		this.token = Optional.ofNullable(token);
	}

}
