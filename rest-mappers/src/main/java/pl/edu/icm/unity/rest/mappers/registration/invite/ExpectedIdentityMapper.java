/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers.registration.invite;

import java.util.Optional;

import io.imunity.rest.api.types.registration.invite.RestExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.authn.ExpectedIdentity.IdentityExpectation;

public class ExpectedIdentityMapper
{
	public static RestExpectedIdentity map(ExpectedIdentity expectedIdentity)
	{
		return RestExpectedIdentity.builder()
				.withIdentity(expectedIdentity.getIdentity())
				.withExpectation(Optional.ofNullable(expectedIdentity.getExpectation())
						.map(Enum::name)
						.orElse(null))
				.build();
	}

	public static ExpectedIdentity map(RestExpectedIdentity restExpectedIdentity)
	{
		return new ExpectedIdentity(restExpectedIdentity.identity, Optional.ofNullable(restExpectedIdentity.expectation)
				.map(IdentityExpectation::valueOf)
				.orElse(null));
	}
}
