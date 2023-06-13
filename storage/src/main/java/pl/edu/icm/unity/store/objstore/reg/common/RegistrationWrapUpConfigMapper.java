/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.Optional;

import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.base.registration.RegistrationWrapUpConfig.TriggeringState;
import pl.edu.icm.unity.store.types.common.I18nStringMapper;

class RegistrationWrapUpConfigMapper
{
	static DBRegistrationWrapUpConfig map(RegistrationWrapUpConfig registrationWrapUpConfig)
	{
		return DBRegistrationWrapUpConfig.builder()
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

	static RegistrationWrapUpConfig map(DBRegistrationWrapUpConfig registrationWrapUpConfig)
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
