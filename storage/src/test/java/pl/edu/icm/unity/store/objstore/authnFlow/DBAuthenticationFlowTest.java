/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.authnFlow;

import java.util.List;
import java.util.Set;

import pl.edu.icm.unity.store.DBTypeTestBase;

public class DBAuthenticationFlowTest extends DBTypeTestBase<DBAuthenticationFlow>
{

	@Override
	protected String getJson()
	{
		return "{\"name\":\"name\",\"firstFactorAuthenticators\":[\"a1\"],"
				+ "\"secondFactorAuthenticators\":[\"a2\"],\"policy\":\"REQUIRE\",\"revision\":0}\n";
	}

	@Override
	protected DBAuthenticationFlow getObject()
	{
		return DBAuthenticationFlow.builder()
				.withName("name")
				.withPolicy("REQUIRE")
				.withRevision(0)
				.withFirstFactorAuthenticators(Set.of("a1"))
				.withSecondFactorAuthenticators(List.of("a2"))
				.build();
	}

}
