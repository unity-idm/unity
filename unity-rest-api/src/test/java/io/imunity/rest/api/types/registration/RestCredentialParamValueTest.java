/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestCredentialParamValueTest extends RestTypeBase<RestCredentialParamValue>
{

	@Override
	protected String getJson()
	{
		return "{\"credentialId\":\"credential\",\"secrets\":\"secret\"}\n";
	}

	@Override
	protected RestCredentialParamValue getObject()
	{
		return RestCredentialParamValue.builder()
				.withCredentialId("credential")
				.withSecrets("secret")
				.build();
	}

}
