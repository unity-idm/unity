/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import io.imunity.rest.api.types.registration.RestCredentialParamValue;
import pl.edu.icm.unity.types.registration.CredentialParamValue;

public class CredentialParamValueMapper
{
	static RestCredentialParamValue map(CredentialParamValue credentialParamValue)
	{
		return RestCredentialParamValue.builder()
				.withCredentialId(credentialParamValue.getCredentialId())
				.withSecrets(credentialParamValue.getSecrets())
				.build();
	}

	static CredentialParamValue map(RestCredentialParamValue restCredentialParamValue)
	{
		return new CredentialParamValue(restCredentialParamValue.credentialId, restCredentialParamValue.secrets);
	}
}
