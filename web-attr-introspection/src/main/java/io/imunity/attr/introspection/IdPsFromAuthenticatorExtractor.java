/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.attr.introspection;

import java.util.Collections;
import java.util.List;

import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificator;
import pl.edu.icm.unity.engine.api.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.types.authn.IdPInfo;

public class IdPsFromAuthenticatorExtractor
{
	public static List<IdPInfo> extractIdPFromAuthenticator(AuthenticatorInstance instance)
	{
		CredentialVerificator verificator = instance.getCredentialVerificator();
		if (verificator instanceof AbstractRemoteVerificator)
		{
			AbstractRemoteVerificator remoteVerificator = (AbstractRemoteVerificator) verificator;
			return remoteVerificator.getIdPs();
		} else
		{
			return Collections.emptyList();
		}
	}
}
