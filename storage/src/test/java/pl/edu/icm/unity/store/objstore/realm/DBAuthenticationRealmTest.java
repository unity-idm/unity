/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.realm;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAuthenticationRealmTest extends DBTypeTestBase<DBAuthenticationRealm>
{

	@Override
	protected String getJson()
	{
		return "{\"description\":\"desc\",\"name\":\"realm\",\"rememberMePolicy\":\"allowFor2ndFactor\","
				+ "\"allowForRememberMeDays\":1,\"blockAfterUnsuccessfulLogins\":1,\"blockFor\":1,\"maxInactivity\":2}";
	}

	@Override
	protected DBAuthenticationRealm getObject()
	{
		return DBAuthenticationRealm.builder()
				.withName("realm")
				.withDescription("desc")
				.withRememberMePolicy("allowFor2ndFactor")
				.withAllowForRememberMeDays(1)
				.withBlockAfterUnsuccessfulLogins(1)
				.withMaxInactivity(2)
				.withBlockFor(1)
				.build();
	}

}
