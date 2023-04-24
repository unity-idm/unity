/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.reg.common;

import java.util.function.Function;

import pl.edu.icm.unity.store.MapperTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.types.registration.RegistrationContext;
import pl.edu.icm.unity.types.registration.RegistrationContext.TriggeringMode;

public class RegistrationContextMapperTest extends MapperTestBase<RegistrationContext, DBRegistrationContext>
{

	@Override
	protected RegistrationContext getFullAPIObject()
	{
		return new RegistrationContext(true, TriggeringMode.manualAtLogin);
	}

	@Override
	protected DBRegistrationContext getFullDBObject()
	{
		return DBRegistrationContext.builder()
				.withIsOnIdpEndpoint(true)
				.withTriggeringMode("manualAtLogin")
				.build();
	}

	@Override
	protected Pair<Function<RegistrationContext, DBRegistrationContext>, Function<DBRegistrationContext, RegistrationContext>> getMapper()
	{
		return Pair.of(RegistrationContextMapper::map, RegistrationContextMapper::map);
	}

}
