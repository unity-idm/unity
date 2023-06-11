/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;

class AuthenticationOptionsSelectorMapper
{
	static DBAuthenticationOptionsSelector map(AuthenticationOptionsSelector authenticationOptionsSelector)
	{
		return DBAuthenticationOptionsSelector.builder()
				.withAuthenticatorKey(authenticationOptionsSelector.authenticatorKey)
				.withOptionKey(authenticationOptionsSelector.optionKey)
				.build();
	}

	static AuthenticationOptionsSelector map(DBAuthenticationOptionsSelector restAuthenticationOptionsSelector)
	{
		return new AuthenticationOptionsSelector(restAuthenticationOptionsSelector.authenticatorKey,
				restAuthenticationOptionsSelector.optionKey);
	}
}