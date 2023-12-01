/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.rest.mappers.registration;

import java.util.function.Function;

import io.imunity.rest.api.types.registration.RestRegistrationContext;
import io.imunity.rest.mappers.MapperTestBase;
import io.imunity.rest.mappers.Pair;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;

public class RegistrationContextMapperTest extends MapperTestBase<RegistrationContext, RestRegistrationContext>
{

	@Override
	protected RegistrationContext getFullAPIObject()
	{
		return new RegistrationContext(true, TriggeringMode.manualAtLogin);
	}

	@Override
	protected RestRegistrationContext getFullRestObject()
	{
		return RestRegistrationContext.builder()
				.withIsOnIdpEndpoint(true)
				.withTriggeringMode("manualAtLogin")
				.build();
	}

	@Override
	protected Pair<Function<RegistrationContext, RestRegistrationContext>, Function<RestRegistrationContext, RegistrationContext>> getMapper()
	{
		return Pair.of(RegistrationContextMapper::map, RegistrationContextMapper::map);
	}

}
