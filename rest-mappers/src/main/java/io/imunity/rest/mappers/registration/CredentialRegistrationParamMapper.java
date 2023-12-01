/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import io.imunity.rest.api.types.registration.RestCredentialRegistrationParam;
import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;

public class CredentialRegistrationParamMapper
{
	public static RestCredentialRegistrationParam map(CredentialRegistrationParam credentialRegistrationParam)
	{
		return RestCredentialRegistrationParam.builder()
				.withCredentialName(credentialRegistrationParam.getCredentialName())
				.withDescription(credentialRegistrationParam.getDescription())
				.withLabel(credentialRegistrationParam.getLabel())
				.build();

	}

	public static CredentialRegistrationParam map(RestCredentialRegistrationParam registrationParam)
	{
		return new CredentialRegistrationParam(registrationParam.credentialName, registrationParam.label,
				registrationParam.description);
	}
}
