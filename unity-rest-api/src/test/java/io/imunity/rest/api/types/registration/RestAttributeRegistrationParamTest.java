/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestAttributeRegistrationParamTest extends RestTypeBase<RestAttributeRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"label\":\"label\",\"description\":\"desc\",\"retrievalSettings\":\"automatic\",\"optional\":false,"
				+ "\"attributeType\":\"type\",\"group\":\"/\",\"showGroups\":true,\"useDescription\":true,"
				+ "\"confirmationMode\":\"CONFIRMED\",\"urlQueryPrefill\":{\"paramName\":\"param\",\"mode\":\"DEFAULT\"}}\n";
	}

	@Override
	protected RestAttributeRegistrationParam getObject()
	{
		return RestAttributeRegistrationParam.builder()
				.withAttributeType("type")
				.withGroup("/")
				.withShowGroups(true)
				.withUseDescription(true)
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
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
