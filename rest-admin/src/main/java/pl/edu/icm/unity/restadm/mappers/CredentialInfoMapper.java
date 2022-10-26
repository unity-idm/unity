/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.stream.Collectors;

import io.imunity.rest.api.types.authn.RestCredentialInfo;
import pl.edu.icm.unity.types.authn.CredentialInfo;

public class CredentialInfoMapper
{
	static RestCredentialInfo map(CredentialInfo credentialInfo)
	{
		return RestCredentialInfo.builder().withCredentialRequirementId(credentialInfo.getCredentialRequirementId())
				.withCredentialsState(credentialInfo.getCredentialsState().entrySet().stream().collect(
						Collectors.toMap(e -> e.getKey(), e -> CredentialPublicInformationMapper.map(e.getValue()))))
				.build();

	}

}
