/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.authn;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;

public class AuthenticationOptionsSelectorMapper
{
	public static RestAuthenticationOptionsSelector map(AuthenticationOptionsSelector authenticationOptionsSelector)
	{
		return RestAuthenticationOptionsSelector.builder()
				.withAuthenticatorKey(authenticationOptionsSelector.authenticatorKey)
				.withOptionKey(authenticationOptionsSelector.optionKey)
				.build();
	}

	public static AuthenticationOptionsSelector map(RestAuthenticationOptionsSelector restAuthenticationOptionsSelector)
	{
		return new AuthenticationOptionsSelector(restAuthenticationOptionsSelector.authenticatorKey,
				restAuthenticationOptionsSelector.optionKey);
	}
}