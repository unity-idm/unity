/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestCredentialRegistrationParam;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;

public class CredentialRegistrationParamTest
		extends MapperTestBase<CredentialRegistrationParam, RestCredentialRegistrationParam>
{

	@Override
	protected CredentialRegistrationParam getFullAPIObject()
	{

		return new CredentialRegistrationParam("name", "label", "desc");
	}

	@Override
	protected RestCredentialRegistrationParam getFullRestObject()
	{

		return RestCredentialRegistrationParam.builder()
				.withCredentialName("name")
				.withDescription("desc")
				.withLabel("label")
				.build();

	}

	@Override
	protected Pair<Function<CredentialRegistrationParam, RestCredentialRegistrationParam>, Function<RestCredentialRegistrationParam, CredentialRegistrationParam>> getMapper()
	{
		return Pair.of(CredentialRegistrationParamMapper::map, CredentialRegistrationParamMapper::map);
	}

}
