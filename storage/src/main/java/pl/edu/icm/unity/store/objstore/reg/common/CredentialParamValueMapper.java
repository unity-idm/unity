/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.base.registration.CredentialParamValue;

public class CredentialParamValueMapper
{
	
	public static DBCredentialParamValue map(CredentialParamValue credentialParamValue)
	{
		return DBCredentialParamValue.builder()
				.withCredentialId(credentialParamValue.getCredentialId())
				.withSecrets(credentialParamValue.getSecrets())
				.build();
	}

	public static CredentialParamValue map(DBCredentialParamValue restCredentialParamValue)
	{
		return new CredentialParamValue(restCredentialParamValue.credentialId, restCredentialParamValue.secrets);
	}
}
