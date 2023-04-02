/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

public class DBRegistrationWrapUpConfigProvider
{
	public static DBRegistrationWrapUpConfig getParam()
	{
		return DBRegistrationWrapUpConfig.builder()
				.withState("AUTO_ACCEPTED")
				.build();
	}
}
