/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.authn.RestAuthenticationOptionsSelector;
import io.imunity.rest.api.types.registration.RestAuthnGridSettings;
import io.imunity.rest.api.types.registration.RestExternalSignupGridSpec;
import pl.edu.icm.unity.base.authn.AuthenticationOptionsSelector;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;
import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec.AuthnGridSettings;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;

public class ExternalSignupGridSpecMapperTest extends MapperTestBase<ExternalSignupGridSpec, RestExternalSignupGridSpec>
{

	@Override
	protected ExternalSignupGridSpec getFullAPIObject()
	{
		return new ExternalSignupGridSpec(List.of(new AuthenticationOptionsSelector("key", "option")),
				new AuthnGridSettings(true, 1));
	}

	@Override
	protected RestExternalSignupGridSpec getFullRestObject()
	{
		return RestExternalSignupGridSpec.builder()
				.withSpecs(List.of(RestAuthenticationOptionsSelector.builder()
						.withAuthenticatorKey("key")
						.withOptionKey("option")
						.build()))
				.withGridSettings(RestAuthnGridSettings.builder()
						.withHeight(1)
						.withSearchable(true)
						.build())
				.build();
	}

	@Override
	protected Pair<Function<ExternalSignupGridSpec, RestExternalSignupGridSpec>, Function<RestExternalSignupGridSpec, ExternalSignupGridSpec>> getMapper()
	{
		return Pair.of(ExternalSignupGridSpecMapper::map, ExternalSignupGridSpecMapper::map);
	}

}
