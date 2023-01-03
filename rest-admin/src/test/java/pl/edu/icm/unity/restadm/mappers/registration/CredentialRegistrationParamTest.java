/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestCredentialRegistrationParam;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.registration.CredentialRegistrationParam;

public class CredentialRegistrationParamTest
		extends MapperTestBase<CredentialRegistrationParam, RestCredentialRegistrationParam>
{

	@Override
	protected CredentialRegistrationParam getAPIObject()
	{

		return new CredentialRegistrationParam("name", "label", "desc");
	}

	@Override
	protected RestCredentialRegistrationParam getRestObject()
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
