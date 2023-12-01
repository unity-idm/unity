/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestRegistrationFormLayouts;
import io.imunity.rest.mappers.registration.layout.FormLayoutMapper;
import pl.edu.icm.unity.types.registration.RegistrationFormLayouts;

public class RegistrationFormLayoutsMapper
{
	public static RestRegistrationFormLayouts map(RegistrationFormLayouts registrationFormLayouts)
	{

		return RestRegistrationFormLayouts.builder()
				.withLocalSignupEmbeddedAsButton(registrationFormLayouts.isLocalSignupEmbeddedAsButton())
				.withPrimaryLayout(Optional.ofNullable(registrationFormLayouts.getPrimaryLayout())
						.map(FormLayoutMapper::map)
						.orElse(null))
				.withSecondaryLayout(Optional.ofNullable(registrationFormLayouts.getSecondaryLayout())
						.map(FormLayoutMapper::map)
						.orElse(null))
				.build();
	}

	public static RegistrationFormLayouts map(RestRegistrationFormLayouts restRegistrationFormLayouts)
	{
		RegistrationFormLayouts registrationFormLayouts = new RegistrationFormLayouts();
		registrationFormLayouts.setLocalSignupEmbeddedAsButton(restRegistrationFormLayouts.localSignupEmbeddedAsButton);
		registrationFormLayouts.setPrimaryLayout(Optional.ofNullable(restRegistrationFormLayouts.primaryLayout)
				.map(FormLayoutMapper::map)
				.orElse(null));
		registrationFormLayouts.setSecondaryLayout(Optional.ofNullable(restRegistrationFormLayouts.secondaryLayout)
				.map(FormLayoutMapper::map)
				.orElse(null));
		return registrationFormLayouts;
	}
}
