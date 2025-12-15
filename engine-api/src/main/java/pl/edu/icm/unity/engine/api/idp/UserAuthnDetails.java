/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.idp;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;

public record UserAuthnDetails(
		RemoteAuthnMetadata firstFactorRemoteIdPAuthnMetadata,
		Set<AuthenticationMethod> authenticationMethods,
		Set<String> authenticatedIdentities,
		List<String> authenticators,
		String remoteIdp)
{
	

}

