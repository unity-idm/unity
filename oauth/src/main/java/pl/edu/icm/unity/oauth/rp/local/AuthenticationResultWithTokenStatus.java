/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;

class AuthenticationResultWithTokenStatus
{
	final AuthenticationResult result;
	final TokenStatus token;

	AuthenticationResultWithTokenStatus(AuthenticationResult result, TokenStatus token)
	{
		this.result = result;
		this.token = token;
	}

}
