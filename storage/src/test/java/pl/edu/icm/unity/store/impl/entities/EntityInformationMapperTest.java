/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Date;
import java.util.function.Function;

import pl.edu.icm.unity.base.identity.EntityInformation;
import pl.edu.icm.unity.base.identity.EntityScheduledOperation;
import pl.edu.icm.unity.base.identity.EntityState;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;

public class EntityInformationMapperTest extends MapperWithMinimalTestBase<EntityInformation, DBEntityInformation>
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
	protected DBEntityInformation getFullDBObject()
	{
		return DBEntityInformation.builder()
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
	protected DBEntityInformation getMinDBObject()
	{

		return DBEntityInformation.builder()
				.withEntityId(1L)
				.withState("valid")
				.build();
	}

	@Override
	protected Pair<Function<EntityInformation, DBEntityInformation>, Function<DBEntityInformation, EntityInformation>> getMapper()
	{
		return Pair.of(EntityInformationMapper::map, EntityInformationMapper::map);
	}

}
