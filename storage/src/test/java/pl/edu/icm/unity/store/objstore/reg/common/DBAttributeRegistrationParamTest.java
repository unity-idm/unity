/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAttributeRegistrationParamTest extends DBTypeTestBase<DBAttributeRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"label\":\"label\",\"description\":\"desc\",\"retrievalSettings\":\"automatic\",\"optional\":false,"
				+ "\"attributeType\":\"type\",\"group\":\"/\",\"showGroups\":true,\"useDescription\":true,"
				+ "\"confirmationMode\":\"CONFIRMED\",\"urlQueryPrefill\":{\"paramName\":\"param\",\"mode\":\"DEFAULT\"}}\n";
	}

	@Override
	protected DBAttributeRegistrationParam getObject()
	{
		return DBAttributeRegistrationParam.builder()
				.withAttributeType("type")
				.withGroup("/")
				.withShowGroups(true)
				.withUseDescription(true)
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withLabel("label")
				.withOptional(false)
				.withRetrievalSettings("automatic")
				.withUrlQueryPrefill(DBurlQueryPrefillConfig.builder()
						.withMode("DEFAULT")
						.withParamName("param")
						.build())
				.build();
	}

}
