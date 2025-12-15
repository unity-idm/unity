/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.RemoteAuthnMetadata;
import pl.edu.icm.unity.engine.api.idp.UserAuthnDetails;

public class UserAuthnDetailsMapper
{
	public static UserAuthnDetails getUserAuthnDetails(SerializableUserAuthnDetails serializableUserAuthnDetails)
	{
		if (serializableUserAuthnDetails == null)
		{
			return new UserAuthnDetails(null, null, null, null, null);
		}

		return new UserAuthnDetails(
				Optional.ofNullable(serializableUserAuthnDetails.firstFactorRemoteIdPAuthnMetadata())
						.map(remoteMeta -> new RemoteAuthnMetadata(remoteMeta.protocol, remoteMeta.remoteIdPId,
								remoteMeta.classReferences))
						.orElse(null),
				serializableUserAuthnDetails.authenticationMethods(),
				serializableUserAuthnDetails.authenticatedIdentities(), serializableUserAuthnDetails.authenticators(),
				serializableUserAuthnDetails.remoteIdp());
	}
}
