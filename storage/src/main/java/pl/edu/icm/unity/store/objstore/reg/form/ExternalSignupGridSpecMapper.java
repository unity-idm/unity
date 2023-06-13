/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.registration.ExternalSignupGridSpec;

class ExternalSignupGridSpecMapper
{
	static DBExternalSignupGridSpec map(ExternalSignupGridSpec externalSignupSpec)
	{
		return DBExternalSignupGridSpec.builder()
				.withSpecs(Optional.ofNullable(externalSignupSpec.getSpecs())
						.map(l -> l.stream()
								.map(AuthenticationOptionsSelectorMapper::map)
								.collect(Collectors.toList()))
						.orElse(null))
				.withGridSettings(Optional.ofNullable(externalSignupSpec.getGridSettings())
						.map(g -> DBAuthnGridSettings.builder()
								.withHeight(g.height)
								.withSearchable(g.searchable)
								.build())
						.orElse(null))
				.build();
	}

	static ExternalSignupGridSpec map(DBExternalSignupGridSpec restExternalSignupSpec)
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
