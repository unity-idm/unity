/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestExternalSignupSpec;
import io.imunity.rest.mappers.authn.AuthenticationOptionsSelectorMapper;
import pl.edu.icm.unity.base.registration.ExternalSignupSpec;

public class ExternalSignupSpecMapper
{
	public static RestExternalSignupSpec map(ExternalSignupSpec externalSignupSpec)
	{
		return RestExternalSignupSpec.builder()
				.withSpecs(Optional.ofNullable(externalSignupSpec.getSpecs())
						.map(l -> l.stream()
								.map(AuthenticationOptionsSelectorMapper::map)
								.collect(Collectors.toList()))
						.orElse(null))

				.build();
	}

	public static ExternalSignupSpec map(RestExternalSignupSpec restExternalSignupSpec)
	{
		return new ExternalSignupSpec(Optional.ofNullable(restExternalSignupSpec.specs)
				.map(l -> l.stream()
						.map(AuthenticationOptionsSelectorMapper::map)
						.collect(Collectors.toList()))
				.orElse(null));
	}

}
