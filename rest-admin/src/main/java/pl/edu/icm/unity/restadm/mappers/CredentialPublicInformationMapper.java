/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;

import io.imunity.rest.api.types.authn.RestCredentialPublicInformation;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;

public class CredentialPublicInformationMapper
{
	static RestCredentialPublicInformation map(CredentialPublicInformation credentialPublicInformation)
	{
		return RestCredentialPublicInformation.builder()
				.withExtraInformation(credentialPublicInformation.getExtraInformation())
				.withState(Optional.ofNullable(credentialPublicInformation.getState()).map(s -> s.name()).orElse(null))
				.withStateDetail(credentialPublicInformation.getStateDetail())
				.build();

	}
}
