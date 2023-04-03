/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Optional;
import java.util.stream.Collectors;

import pl.edu.icm.unity.types.registration.ExternalSignupSpec;

class ExternalSignupSpecMapper
{
	static DBExternalSignupSpec map(ExternalSignupSpec externalSignupSpec)
	{
		return DBExternalSignupSpec.builder()
				.withSpecs(Optional.ofNullable(externalSignupSpec.getSpecs())
						.map(l -> l.stream()
								.map(AuthenticationOptionsSelectorMapper::map)
								.collect(Collectors.toList()))
						.orElse(null))

				.build();
	}

	static ExternalSignupSpec map(DBExternalSignupSpec restExternalSignupSpec)
	{
		return new ExternalSignupSpec(Optional.ofNullable(restExternalSignupSpec.specs)
				.map(l -> l.stream()
						.map(AuthenticationOptionsSelectorMapper::map)
						.collect(Collectors.toList()))
				.orElse(null));
	}

}
