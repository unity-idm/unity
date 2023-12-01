/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestCredentialParamValue;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.types.registration.CredentialParamValue;

public class CredentialParamValueMapperTest extends MapperTestBase<CredentialParamValue, RestCredentialParamValue>
{

	@Override
	protected CredentialParamValue getFullAPIObject()
	{
		return new CredentialParamValue("credential", "secret");
	}

	@Override
	protected RestCredentialParamValue getFullRestObject()
	{
		return RestCredentialParamValue.builder()
				.withCredentialId("credential")
				.withSecrets("secret")
				.build();
	}

	@Override
	protected Pair<Function<CredentialParamValue, RestCredentialParamValue>, Function<RestCredentialParamValue, CredentialParamValue>> getMapper()
	{
		return Pair.of(CredentialParamValueMapper::map, CredentialParamValueMapper::map);
	}

}
