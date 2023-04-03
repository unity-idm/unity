/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.identitytype;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBIdentityTypeTest extends DBTypeTestBase<DBIdentityType>
{

	@Override
	protected String getJson()
	{
		return "{\"identityTypeProvider\":\"typeProvider\",\"description\":\"desc\",\"selfModificable\":true,"
				+ "\"minInstances\":1,\"maxInstances\":1,\"minVerifiedInstances\":2,"
				+ "\"identityTypeProviderSettings\":\"providerSettings\","
				+ "\"emailConfirmationConfiguration\":{\"messageTemplate\":\"template\",\"validityTime\":10},"
				+ "\"name\":\"type\"}\n";
	}

	@Override
	protected DBIdentityType getObject()
	{
		return DBIdentityType.builder()
				.withDescription("desc")
				.withEmailConfirmationConfiguration(DBEmailConfirmationConfiguration.builder()
				.withValidityTime(10)
				.withMessageTemplate("template")
				.build())
				.withIdentityTypeProvider("typeProvider")
				.withIdentityTypeProviderSettings("providerSettings")
				.withMaxInstances(1)
				.withMinInstances(1)
				.withMinVerifiedInstances(2)
				.withName("type")
				.withSelfModificable(true)
				.build();
	}

}
