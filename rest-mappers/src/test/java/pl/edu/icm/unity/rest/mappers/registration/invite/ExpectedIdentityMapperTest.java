/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration.invite;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.invite.RestExpectedIdentity;
import pl.edu.icm.unity.rest.mappers.MapperTestBase;
import pl.edu.icm.unity.rest.mappers.Pair;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;

public class ExpectedIdentityMapperTest extends MapperTestBase<ExpectedIdentity, RestExpectedIdentity>
{

	@Override
	protected ExpectedIdentity getFullAPIObject()
	{
		return new ExpectedIdentity("identity", IdentityExpectation.MANDATORY);
	}

	@Override
	protected RestExpectedIdentity getFullRestObject()
	{
		return RestExpectedIdentity.builder()
				.withExpectation("MANDATORY")
				.withIdentity("identity")
				.build();
	}

	@Override
	protected Pair<Function<ExpectedIdentity, RestExpectedIdentity>, Function<RestExpectedIdentity, ExpectedIdentity>> getMapper()
	{
		return Pair.of(ExpectedIdentityMapper::map, ExpectedIdentityMapper::map);
	}

}
