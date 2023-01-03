/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.api.types.registration;

import io.imunity.rest.api.types.basic.RestTypeBase;

public class RestGroupRegistrationParamTest extends RestTypeBase<RestGroupRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"label\":\"label\",\"description\":\"desc\",\"retrievalSettings\":\"automatic\","
				+ "\"groupPath\":\"/group\",\"multiSelect\":true,\"includeGroupsMode\":\"all\"}\n";
	}

	@Override
	protected RestGroupRegistrationParam getObject()
	{
		return RestGroupRegistrationParam.builder()
				.withGroupPath("/group")
				.withDescription("desc")
				.withIncludeGroupsMode("all")
				.withMultiSelect(true)
				.withLabel("label")
				.withRetrievalSettings("automatic")
				.build();
	}

}
