/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.util.Optional;
import java.util.stream.Collectors;

import io.imunity.rest.api.types.registration.RestAuthnGridSettings;
import io.imunity.rest.api.types.registration.RestExternalSignupGridSpec;
import pl.edu.icm.unity.restadm.mappers.authn.AuthenticationOptionsSelectorMapper;
import pl.edu.icm.unity.types.registration.ExternalSignupGridSpec;

public class ExternalSignupGridSpecMapper
{
	public static RestExternalSignupGridSpec map(ExternalSignupGridSpec externalSignupSpec)
	{
		return RestExternalSignupGridSpec.builder()
				.withSpecs(Optional.ofNullable(externalSignupSpec.getSpecs())
						.map(l -> l.stream()
								.map(AuthenticationOptionsSelectorMapper::map)
								.collect(Collectors.toList()))
						.orElse(null))
				.withGridSettings(Optional.ofNullable(externalSignupSpec.getGridSettings())
						.map(g -> RestAuthnGridSettings.builder()
								.withHeight(g.height)
								.withSearchable(g.searchable)
								.build())
						.orElse(null))
				.build();
	}

	public static ExternalSignupGridSpec map(RestExternalSignupGridSpec restExternalSignupSpec)
	{
		return new ExternalSignupGridSpec(Optional.ofNullable(restExternalSignupSpec.specs)
				.map(l -> l.stream()
						.map(AuthenticationOptionsSelectorMapper::map)
						.collect(Collectors.toList()))
				.orElse(null),
				Optional.ofNullable(restExternalSignupSpec.gridSettings)
						.map(g -> new ExternalSignupGridSpec.AuthnGridSettings(g.searchable, g.height))
						.orElse(null));
	}

}
