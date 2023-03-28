/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.invite;

import java.util.Optional;

import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;

public class ExpectedIdentityMapper
{
	public static DBExpectedIdentity map(ExpectedIdentity expectedIdentity)
	{
		return DBExpectedIdentity.builder()
				.withIdentity(expectedIdentity.getIdentity())
				.withExpectation(Optional.ofNullable(expectedIdentity.getExpectation())
						.map(Enum::name)
						.orElse(null))
				.build();
	}

	public static ExpectedIdentity map(DBExpectedIdentity restExpectedIdentity)
	{
		return new ExpectedIdentity(restExpectedIdentity.identity, Optional.ofNullable(restExpectedIdentity.expectation)
				.map(IdentityExpectation::valueOf)
				.orElse(null));
	}
}
