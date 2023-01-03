/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.authn;

import java.util.Map;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestCredentialInfoTest extends RestTypeBase<RestCredentialInfo>
{
	@Override
	protected String getJson()
	{
		return "{\"credentialRequirementId\":\"credreq1\",\"credentialsState\":{\"test\":{\"state\":\"correct\",\"stateDetail\":\"state\","
				+ "\"extraInformation\":\"state\"}}}\n";

	}

	@Override
	protected RestCredentialInfo getObject()
	{

		return RestCredentialInfo.builder()
				.withCredentialRequirementId("credreq1")
				.withCredentialsState(Map.of("test", RestCredentialPublicInformation.builder()
						.withState("correct")
						.withStateDetail("state")
						.withExtraInformation("state")
						.build()))
				.build();
	}
}
