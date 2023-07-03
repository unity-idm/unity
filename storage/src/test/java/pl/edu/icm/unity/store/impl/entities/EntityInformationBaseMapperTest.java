/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Date;
import java.util.function.Function;

import pl.edu.icm.unity.base.entity.EntityInformation;
import pl.edu.icm.unity.base.entity.EntityScheduledOperation;
import pl.edu.icm.unity.base.entity.EntityState;
import pl.edu.icm.unity.store.MapperWithMinimalTestBase;
import pl.edu.icm.unity.store.Pair;

public class EntityInformationBaseMapperTest extends MapperWithMinimalTestBase<EntityInformation, DBEntityInformationBase>
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
	protected DBEntityInformationBase getFullDBObject()
	{
		return DBEntityInformationBase.builder()
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
	protected DBEntityInformationBase getMinDBObject()
	{

		return DBEntityInformationBase.builder()
				.withState("valid")
				.build();
	}

	@Override
	protected Pair<Function<EntityInformation, DBEntityInformationBase>, Function<DBEntityInformationBase, EntityInformation>> getMapper()
	{
		return Pair.of(EntityInformationBaseMapper::map, e -> EntityInformationBaseMapper.map(e, 1));
	}

}
