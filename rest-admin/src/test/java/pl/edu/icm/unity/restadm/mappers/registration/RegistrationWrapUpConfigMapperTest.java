/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers.registration;

import java.time.Duration;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestI18nString;
import io.imunity.rest.api.types.registration.RestRegistrationWrapUpConfig;
import pl.edu.icm.unity.restadm.mappers.MapperTestBase;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;

public class RegistrationWrapUpConfigMapperTest
		extends MapperTestBase<RegistrationWrapUpConfig, RestRegistrationWrapUpConfig>
{

	@Override
	protected RegistrationWrapUpConfig getFullAPIObject()
	{
		return new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED, new I18nString("title"),
				new I18nString("info"), new I18nString("redirect"), true, "redirectUrl", Duration.ofDays(1));
	}

	@Override
	protected RestRegistrationWrapUpConfig getFullRestObject()
	{
		return RestRegistrationWrapUpConfig.builder()
				.withAutomatic(true)
				.withInfo(RestI18nString.builder()
						.withDefaultValue("info")
						.build())
				.withRedirectCaption(RestI18nString.builder()
						.withDefaultValue("redirect")
						.build())
				.withTitle(RestI18nString.builder()
						.withDefaultValue("title")
						.build())
				.withRedirectAfterTime(Duration.ofDays(1))
				.withState("AUTO_ACCEPTED")
				.withRedirectURL("redirectUrl")
				.build();
	}

	@Override
	protected Pair<Function<RegistrationWrapUpConfig, RestRegistrationWrapUpConfig>, Function<RestRegistrationWrapUpConfig, RegistrationWrapUpConfig>> getMapper()
	{
		return Pair.of(RegistrationWrapUpConfigMapper::map, RegistrationWrapUpConfigMapper::map);
	}
}
