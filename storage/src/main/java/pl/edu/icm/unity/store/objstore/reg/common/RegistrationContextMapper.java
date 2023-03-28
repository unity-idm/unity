/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;

public class RegistrationContextMapper
{
	public static DBRegistrationContext map(RegistrationContext registrationContext)
	{
		return DBRegistrationContext.builder()
				.withIsOnIdpEndpoint(registrationContext.isOnIdpEndpoint)
				.withTriggeringMode(Optional.ofNullable(registrationContext.triggeringMode)
						.map(t -> t.name())
						.orElse(null))
				.build();
	}

	public static RegistrationContext map(DBRegistrationContext restRegistrationContext)
	{
		return new RegistrationContext(restRegistrationContext.isOnIdpEndpoint,
				Optional.ofNullable(restRegistrationContext.triggeringMode)
						.map(TriggeringMode::valueOf)
						.orElse(null));
	}
}
