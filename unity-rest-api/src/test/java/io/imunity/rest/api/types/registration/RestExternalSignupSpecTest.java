/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestExternalSignupSpecTest extends RestTypeBase<RestExternalSignupSpec>
{

	@Override
	protected String getJson()
	{
		return "{\"specs\":[{\"authenticatorKey\":\"key\",\"optionKey\":\"option\"}]}\n";
	}

	@Override
	protected RestExternalSignupSpec getObject()
	{
		return RestExternalSignupSpec.builder()
				.withSpecs(List.of(RestAuthenticationOptionsSelector.builder().withAuthenticatorKey("key")
						.withOptionKey("option").build()))
				.build();
	}

}
