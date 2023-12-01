/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.Optional;

import io.imunity.rest.api.types.registration.RestRegistrationWrapUpConfig;
import io.imunity.rest.mappers.I18nStringMapper;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;

public class RegistrationWrapUpConfigMapper
{
	public static RestRegistrationWrapUpConfig map(RegistrationWrapUpConfig registrationWrapUpConfig)
	{
		return RestRegistrationWrapUpConfig.builder()
				.withAutomatic(registrationWrapUpConfig.isAutomatic())
				.withState(registrationWrapUpConfig.getState()
						.name())
				.withRedirectURL(registrationWrapUpConfig.getRedirectURL())
				.withRedirectAfterTime(registrationWrapUpConfig.getRedirectAfterTime())
				.withInfo(Optional.ofNullable(registrationWrapUpConfig.getInfo())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withTitle(Optional.ofNullable(registrationWrapUpConfig.getTitle())
						.map(I18nStringMapper::map)
						.orElse(null))
				.withRedirectCaption(Optional.ofNullable(registrationWrapUpConfig.getRedirectCaption())
						.map(I18nStringMapper::map)
						.orElse(null))
				.build();
	}

	public static RegistrationWrapUpConfig map(RestRegistrationWrapUpConfig registrationWrapUpConfig)
	{
		return new RegistrationWrapUpConfig(TriggeringState.valueOf(registrationWrapUpConfig.state),
				Optional.ofNullable(registrationWrapUpConfig.title)
						.map(I18nStringMapper::map)
						.orElse(null),
				Optional.ofNullable(registrationWrapUpConfig.info)
						.map(I18nStringMapper::map)
						.orElse(null),
				Optional.ofNullable(registrationWrapUpConfig.redirectCaption)
						.map(I18nStringMapper::map)
						.orElse(null),
				registrationWrapUpConfig.automatic, registrationWrapUpConfig.redirectURL,
				registrationWrapUpConfig.redirectAfterTime);
	}

}
