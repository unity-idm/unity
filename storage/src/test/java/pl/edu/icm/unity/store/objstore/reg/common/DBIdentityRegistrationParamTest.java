/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBIdentityRegistrationParamTest extends DBTypeTestBase<DBIdentityRegistrationParam>
{

	@Override
	protected String getJson()
	{
		return "{\"label\":\"label\",\"description\":\"desc\",\"retrievalSettings\":\"automatic\","
				+ "\"optional\":false,\"identityType\":\"type\",\"confirmationMode\":\"CONFIRMED\","
				+ "\"urlQueryPrefill\":{\"paramName\":\"param\",\"mode\":\"DEFAULT\"}}\n";
	}

	@Override
	protected DBIdentityRegistrationParam getObject()
	{
		return DBIdentityRegistrationParam.builder()
				.withConfirmationMode("CONFIRMED")
				.withDescription("desc")
				.withIdentityType("type")
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
