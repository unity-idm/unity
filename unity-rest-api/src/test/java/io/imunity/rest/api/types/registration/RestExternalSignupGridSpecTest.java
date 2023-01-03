/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import java.util.List;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestExternalSignupGridSpecTest extends RestTypeBase<RestExternalSignupGridSpec>
{

	@Override
	protected String getJson()
	{
		return "{\"specs\":[{\"authenticatorKey\":\"key\",\"optionKey\":\"option\"}],"
				+ "\"gridSettings\":{\"searchable\":true,\"height\":1}}\n";
	}

	@Override
	protected RestExternalSignupGridSpec getObject()
	{
		return RestExternalSignupGridSpec.builder()
				.withSpecs(List.of(RestAuthenticationOptionsSelector.builder()
						.withAuthenticatorKey("key")
						.withOptionKey("option")
						.build()))
				.withGridSettings(RestAuthnGridSettings.builder()
						.withHeight(1)
						.withSearchable(true)
						.build())
				.build();
	}

}
