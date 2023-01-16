/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration.invite;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.invite.RestExpectedIdentity;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;

public class ExpectedIdentityMapperTest extends MapperTestBase<ExpectedIdentity, RestExpectedIdentity>
{

	@Override
	protected ExpectedIdentity getAPIObject()
	{
		return new ExpectedIdentity("identity", IdentityExpectation.MANDATORY);
	}

	@Override
	protected RestExpectedIdentity getRestObject()
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
