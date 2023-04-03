/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

public class DBIdentityRegistrationParamProvider
{
	public static DBIdentityRegistrationParam getParam()
	{
		return DBIdentityRegistrationParam.builder()
				.withIdentityType("userName")
				.withRetrievalSettings("automaticHidden")
				.withConfirmationMode("ON_SUBMIT")
				.build();
	}
}
