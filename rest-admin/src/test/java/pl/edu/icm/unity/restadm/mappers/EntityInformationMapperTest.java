/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Date;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import io.imunity.rest.api.types.basic.RestEntityInformation;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;

public class EntityInformationMapperTest extends MapperWithMinimalTestBase<EntityInformation, RestEntityInformation>
{

	@Override
	protected EntityInformation getFullAPIObject()
	{
		EntityInformation entityInformation = new EntityInformation(1);
		entityInformation.setState(EntityState.valid);
		entityInformation.setRemovalByUserTime(new Date(1));
		entityInformation.setScheduledOperationTime(new Date(2));
		entityInformation.setScheduledOperation(EntityScheduledOperation.DISABLE);
		return entityInformation;

	}

	@Override
	protected RestEntityInformation getFullRestObject()
	{
		return RestEntityInformation.builder()
				.withEntityId(1L)
				.withRemovalByUserTime(new Date(1))
				.withScheduledOperationTime(new Date(2))
				.withScheduledOperation("DISABLE")
				.withState("valid")
				.build();
	}

	@Override
	protected EntityInformation getMinAPIObject()
	{

		return new EntityInformation(1);
	}

	@Override
	protected RestEntityInformation getMinRestObject()
	{

		return RestEntityInformation.builder()
				.withEntityId(1L)
				.withState("valid")
				.build();
	}

	@Override
	protected Pair<Function<EntityInformation, RestEntityInformation>, Function<RestEntityInformation, EntityInformation>> getMapper()
	{
		return Pair.of(EntityInformationMapper::map, EntityInformationMapper::map);
	}

}
