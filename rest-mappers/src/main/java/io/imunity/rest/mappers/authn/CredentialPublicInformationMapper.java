/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.authn;

import java.util.Optional;

import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import pl.edu.icm.unity.base.authn.CredentialPublicInformation;
import pl.edu.icm.unity.base.authn.LocalCredentialState;

public class CredentialPublicInformationMapper
{
	public static RestCredentialPublicInformation map(CredentialPublicInformation credentialPublicInformation)
	{
		return RestCredentialPublicInformation.builder()
				.withExtraInformation(credentialPublicInformation.getExtraInformation())
				.withState(Optional.ofNullable(credentialPublicInformation.getState())
						.map(s -> s.name())
						.orElse(null))
				.withStateDetail(credentialPublicInformation.getStateDetail())
				.build();

	}

	public static CredentialPublicInformation map(RestCredentialPublicInformation restCredentialPublicInformation)
	{
		return new CredentialPublicInformation(LocalCredentialState.valueOf(restCredentialPublicInformation.state),
				restCredentialPublicInformation.stateDetail, restCredentialPublicInformation.extraInformation);

	}
}
