/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.form;

import java.util.Optional;

import pl.edu.icm.unity.base.registration.RegistrationFormLayouts;
import pl.edu.icm.unity.store.objstore.reg.layout.FormLayoutMapper;

class RegistrationFormLayoutsMapper
{
	 static DBRegistrationFormLayouts map(RegistrationFormLayouts registrationFormLayouts)
	{

		return DBRegistrationFormLayouts.builder()
				.withLocalSignupEmbeddedAsButton(registrationFormLayouts.isLocalSignupEmbeddedAsButton())
				.withPrimaryLayout(Optional.ofNullable(registrationFormLayouts.getPrimaryLayout())
						.map(FormLayoutMapper::map)
						.orElse(null))
				.withSecondaryLayout(Optional.ofNullable(registrationFormLayouts.getSecondaryLayout())
						.map(FormLayoutMapper::map)
						.orElse(null))
				.build();
	}

	 static RegistrationFormLayouts map(DBRegistrationFormLayouts restRegistrationFormLayouts)
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
