/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.authn.RestCredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;

public class CredentialInfoMapper
{
	static RestCredentialInfo map(CredentialInfo credentialInfo)
	{
		return RestCredentialInfo.builder()
				.withCredentialRequirementId(credentialInfo.getCredentialRequirementId())
				.withCredentialsState(Optional.ofNullable(credentialInfo.getCredentialsState())
						.map(cs -> cs.entrySet()
								.stream()
								.collect(Collectors.toMap(e -> e.getKey(),
										e -> CredentialPublicInformationMapper.map(e.getValue()))))
						.orElse(null))
				.build();

	}

	static CredentialInfo map(RestCredentialInfo restCredentialInfo)
	{
		return new CredentialInfo(restCredentialInfo.credentialRequirementId, Optional
				.ofNullable(restCredentialInfo.credentialsState)
				.map(s -> s.entrySet()
						.stream()
						.collect(Collectors.toMap(e -> e.getKey(),
								e -> new CredentialPublicInformation(LocalCredentialState.valueOf(e.getValue().state),
										e.getValue().stateDetail, e.getValue().extraInformation))))
				.orElse(null));

	}

}
