/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.time.Duration;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig;
import pl.edu.icm.unity.types.registration.RegistrationWrapUpConfig.TriggeringState;

public class RegistrationWrapUpConfigMapperTest
		extends MapperTestBase<RegistrationWrapUpConfig, DBRegistrationWrapUpConfig>
{

	@Override
	protected RegistrationWrapUpConfig getFullAPIObject()
	{
		return new RegistrationWrapUpConfig(TriggeringState.AUTO_ACCEPTED, new I18nString("title"),
				new I18nString("info"), new I18nString("redirect"), true, "redirectUrl", Duration.ofDays(1));
	}

	@Override
	protected DBRegistrationWrapUpConfig getFullDBObject()
	{
		return DBRegistrationWrapUpConfig.builder()
				.withAutomatic(true)
				.withInfo(DBI18nString.builder()
						.withDefaultValue("info")
						.build())
				.withRedirectCaption(DBI18nString.builder()
						.withDefaultValue("redirect")
						.build())
				.withTitle(DBI18nString.builder()
						.withDefaultValue("title")
						.build())
				.withRedirectAfterTime(Duration.ofDays(1))
				.withState("AUTO_ACCEPTED")
				.withRedirectURL("redirectUrl")
				.build();
	}

	@Override
	protected Pair<Function<RegistrationWrapUpConfig, DBRegistrationWrapUpConfig>, Function<DBRegistrationWrapUpConfig, RegistrationWrapUpConfig>> getMapper()
	{
		return Pair.of(RegistrationWrapUpConfigMapper::map, RegistrationWrapUpConfigMapper::map);
	}
}
