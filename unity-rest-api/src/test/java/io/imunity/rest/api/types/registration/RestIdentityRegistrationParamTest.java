/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestIdentityRegistrationParamTest extends RestTypeBase<RestIdentityRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"label\":\"label\",\"description\":\"desc\",\"retrievalSettings\":\"automatic\","
				+ "\"optional\":false,\"identityType\":\"type\",\"confirmationMode\":\"CONFIRMED\","
				+ "\"urlQueryPrefill\":{\"paramName\":\"param\",\"mode\":\"DEFAULT\"}}\n";
	}

	@Override
	protected RestIdentityRegistrationParam getObject()
	{
		return RestIdentityRegistrationParam.builder()
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withIdentityType("type")
				.withLabel("label")
				.withOptional(false)
				.withRetrievalSettings("automatic")
				.withUrlQueryPrefill(RestURLQueryPrefillConfig.builder()
						.withMode("DEFAULT")
						.withParamName("param")
						.build())
				.build();
	}

}
