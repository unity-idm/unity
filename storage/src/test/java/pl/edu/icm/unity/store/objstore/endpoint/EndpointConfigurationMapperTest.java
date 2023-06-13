/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;

public class EndpointConfigurationMapperTest extends MapperWithMinimalTestBase<EndpointConfiguration, DBEndpointConfiguration>
{

	@Override
	protected EndpointConfiguration getFullAPIObject()
	{
		return new EndpointConfiguration(new I18nString("disp"), "desc", List.of("ao1"), "conf", "realm", "tag1");
	}

	@Override
	protected DBEndpointConfiguration getFullDBObject()
	{
		return DBEndpointConfiguration.builder()
				.withDisplayedName(DBI18nString.builder()
						.withDefaultValue("disp")
						.build())
				.withDescription("desc")
				.withRealm("realm")
				.withTag("tag1")
				.withAuthenticationOptions(List.of("ao1"))
				.withConfiguration("conf")
				.build();
	}

	@Override
	protected EndpointConfiguration getMinAPIObject()
	{
		return new EndpointConfiguration(null, null, null, null, null);
	}

	@Override
	protected DBEndpointConfiguration getMinDBObject()
	{
		return DBEndpointConfiguration.builder()		
				.withTag("")
				.build();
	}

	@Override
	protected Pair<Function<EndpointConfiguration, DBEndpointConfiguration>, Function<DBEndpointConfiguration, EndpointConfiguration>> getMapper()
	{
		return Pair.of(EndpointConfigurationMapper::map, EndpointConfigurationMapper::map);
	}
}
