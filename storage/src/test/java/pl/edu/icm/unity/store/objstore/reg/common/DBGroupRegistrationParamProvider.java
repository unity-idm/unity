/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

public class DBGroupRegistrationParamProvider
{
	public static DBGroupRegistrationParam getParam()
	{
		return DBGroupRegistrationParam.builder()
				.withGroupPath("/B")
				.withRetrievalSettings("automatic")
				.withIncludeGroupsMode("all")
				.withMultiSelect(false)
				.build();
	}
}
