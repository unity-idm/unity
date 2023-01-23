/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.registration.RestCredentialParamValue;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
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
