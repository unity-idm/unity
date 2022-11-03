/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.authn;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestCredentialPublicInformationTest extends RestTypeBase<RestCredentialPublicInformation>
{

	@Override
	protected String getJson()
	{
		return "{\"state\":\"correct\",\"stateDetail\":\"state\",\"extraInformation\":\"state\"}\n";
	}

	@Override
	protected RestCredentialPublicInformation getObject()
	{
		return RestCredentialPublicInformation.builder().withState("correct").withStateDetail("state")
				.withExtraInformation("state").build();
	}
}
