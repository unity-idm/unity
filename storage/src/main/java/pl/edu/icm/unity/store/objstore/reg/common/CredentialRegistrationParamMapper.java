/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.base.registration.CredentialRegistrationParam;

class CredentialRegistrationParamMapper
{
	static DBCredentialRegistrationParam map(CredentialRegistrationParam credentialRegistrationParam)
	{
		return DBCredentialRegistrationParam.builder()
				.withCredentialName(credentialRegistrationParam.getCredentialName())
				.withDescription(credentialRegistrationParam.getDescription())
				.withLabel(credentialRegistrationParam.getLabel())
				.build();

	}

	static CredentialRegistrationParam map(DBCredentialRegistrationParam registrationParam)
	{
		return new CredentialRegistrationParam(registrationParam.credentialName, registrationParam.label,
				registrationParam.description);
	}
}
