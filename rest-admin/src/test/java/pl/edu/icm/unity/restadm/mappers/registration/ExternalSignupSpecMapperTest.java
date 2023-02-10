/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import io.imunity.rest.api.types.registration.RestExternalSignupSpec;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.types.registration.ExternalSignupSpec;

public class ExternalSignupSpecMapperTest extends MapperTestBase<ExternalSignupSpec, RestExternalSignupSpec>
{

	@Override
	protected ExternalSignupSpec getFullAPIObject()
	{
		return new ExternalSignupSpec(List.of(new AuthenticationOptionsSelector("key", "option")));
	}

	@Override
	protected RestExternalSignupSpec getFullRestObject()
	{
		return RestExternalSignupSpec.builder()
				.withSpecs(List.of(RestAuthenticationOptionsSelector.builder()
						.withAuthenticatorKey("key")
						.withOptionKey("option")
						.build()))
				.build();
	}

	@Override
	protected Pair<Function<ExternalSignupSpec, RestExternalSignupSpec>, Function<RestExternalSignupSpec, ExternalSignupSpec>> getMapper()
	{
		return Pair.of(ExternalSignupSpecMapper::map, ExternalSignupSpecMapper::map);
	}

}
