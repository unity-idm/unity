/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBGroupRegistrationParamTest extends DBTypeTestBase<DBGroupRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"label\":\"label\",\"description\":\"desc\",\"retrievalSettings\":\"automatic\","
				+ "\"groupPath\":\"/group\",\"multiSelect\":true,\"includeGroupsMode\":\"all\"}\n";
	}

	@Override
	protected DBGroupRegistrationParam getObject()
	{
		return DBGroupRegistrationParam.builder()
				.withGroupPath("/group")
				.withDescription("desc")
				.withIncludeGroupsMode("all")
				.withMultiSelect(true)
				.withLabel("label")
				.withRetrievalSettings("automatic")
				.build();
	}

}
