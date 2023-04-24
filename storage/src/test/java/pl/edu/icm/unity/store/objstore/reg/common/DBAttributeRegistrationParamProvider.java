/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

public class DBAttributeRegistrationParamProvider
{
	public static DBAttributeRegistrationParam getParam()

	{
		return DBAttributeRegistrationParam.builder()
				.withAttributeType("email")
				.withGroup("/")
				.withOptional(true)
				.withRetrievalSettings("interactive")
				.withShowGroups(true)
				.withConfirmationMode("ON_SUBMIT")
				.build();
	}
}
