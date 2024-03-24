/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.authnFlow;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;

public class AuthenticationFlowMapperTest extends MapperTestBase<AuthenticationFlowDefinition, DBAuthenticationFlow>
{

	@Override
	protected AuthenticationFlowDefinition getFullAPIObject()
	{
		return new AuthenticationFlowDefinition("name", Policy.REQUIRE, Set.of("a1"), List.of("a2"), "config");
	}

	@Override
	protected DBAuthenticationFlow getFullDBObject()
	{
		return DBAuthenticationFlow.builder()
				.withName("name")
				.withPolicy("REQUIRE")
				.withRevision(0)
				.withFirstFactorAuthenticators(Set.of("a1"))
				.withSecondFactorAuthenticators(List.of("a2"))
				.withPolicyConfiguration("config")
				.build();

	}

	@Override
	protected Pair<Function<AuthenticationFlowDefinition, DBAuthenticationFlow>, Function<DBAuthenticationFlow, AuthenticationFlowDefinition>> getMapper()
	{
		return Pair.of(AuthenticationFlowMapper::map, AuthenticationFlowMapper::map);
	}

}
