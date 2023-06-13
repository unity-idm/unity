/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.function.Function;

import pl.edu.icm.unity.base.authn.ExpectedIdentity;
import pl.edu.icm.unity.base.authn.ExpectedIdentity.IdentityExpectation;
import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;

public class ExpectedIdentityMapperTest extends MapperTestBase<ExpectedIdentity, DBExpectedIdentity>
{

	@Override
	protected ExpectedIdentity getFullAPIObject()
	{
		return new ExpectedIdentity("identity", IdentityExpectation.MANDATORY);
	}

	@Override
	protected DBExpectedIdentity getFullDBObject()
	{
		return DBExpectedIdentity.builder()
				.withExpectation("MANDATORY")
				.withIdentity("identity")
				.build();
	}

	@Override
	protected Pair<Function<ExpectedIdentity, DBExpectedIdentity>, Function<DBExpectedIdentity, ExpectedIdentity>> getMapper()
	{
		return Pair.of(ExpectedIdentityMapper::map, ExpectedIdentityMapper::map);
	}

}
