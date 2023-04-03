/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.objstore.endpoint;

import java.util.List;
import java.util.function.Function;

import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;
import pl.edu.icm.unity.store.types.common.DBI18nString;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.endpoint.Endpoint;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;

public class EndpointMapperTest extends MapperWithMinimalTestBase<Endpoint, DBEndpoint>
{

	@Override
	protected Endpoint getFullAPIObject()
	{
		return new Endpoint("endpoint", "rest", "/rest",
				new EndpointConfiguration(new I18nString("disp"), "desc", List.of("ao1"), "conf", "realm", "tag1"), 1);
	}

	@Override
	protected DBEndpoint getFullDBObject()
	{
		return DBEndpoint.builder()
				.withConfiguration(DBEndpointConfiguration.builder()
						.withDisplayedName(DBI18nString.builder()
								.withDefaultValue("disp")
								.build())
						.withDescription("desc")
						.withRealm("realm")
						.withTag("tag1")
						.withAuthenticationOptions(List.of("ao1"))
						.withConfiguration("conf")
						.build())
				.withName("endpoint")
				.withTypeId("rest")
				.withRevision(1)
				.withStatus("DEPLOYED")
				.withContextAddress("/rest")
				.build();
	}

	@Override
	protected Endpoint getMinAPIObject()
	{

		return new Endpoint("endpoint", null, null, null, 0);
	}

	@Override
	protected DBEndpoint getMinDBObject()
	{
		return DBEndpoint.builder()
				.withName("endpoint")
				.withRevision(0)
				.build();
	}

	@Override
	protected Pair<Function<Endpoint, DBEndpoint>, Function<DBEndpoint, Endpoint>> getMapper()
	{
		return Pair.of(EndpointMapper::map, EndpointMapper::map);
	}
}
