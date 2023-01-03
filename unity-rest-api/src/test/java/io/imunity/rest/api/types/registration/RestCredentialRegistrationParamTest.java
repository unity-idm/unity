/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestCredentialRegistrationParamTest extends RestTypeBase<RestCredentialRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"credentialName\":\"name\",\"label\":\"label\",\"description\":\"desc\"}\n";
	}

	@Override
	protected RestCredentialRegistrationParam getObject()
	{
		return RestCredentialRegistrationParam.builder()
				.withCredentialName("name")
				.withDescription("desc")
				.withLabel("label")
				.build();
	}

}
