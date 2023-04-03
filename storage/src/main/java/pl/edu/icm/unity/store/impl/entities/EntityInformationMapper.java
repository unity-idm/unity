/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Optional;

import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;

class EntityInformationMapper
{
	static DBEntityInformation map(EntityInformation entityInformation)
	{
		return DBEntityInformation.builder()
				.withEntityId(entityInformation.getId())
				.withState(Optional.ofNullable(entityInformation.getEntityState())
						.map(s -> s.name())
						.orElse(null))
				.withRemovalByUserTime(entityInformation.getRemovalByUserTime())
				.withScheduledOperationTime(entityInformation.getScheduledOperationTime())
				.withScheduledOperation(Optional.ofNullable(entityInformation.getScheduledOperation())
						.map(s -> s.name())
						.orElse(null))
				.build();

	}

	static EntityInformation map(DBEntityInformation dbEntityInformation)
	{
		EntityInformation entityInformation = new EntityInformation(dbEntityInformation.entityId);
		entityInformation.setEntityState(Optional.ofNullable(dbEntityInformation.state)
				.map(s -> EntityState.valueOf(s))
				.orElse(null));
		entityInformation.setRemovalByUserTime(dbEntityInformation.removalByUserTime);
		entityInformation.setScheduledOperation(Optional.ofNullable(dbEntityInformation.scheduledOperation)
				.map(s -> EntityScheduledOperation.valueOf(s))
				.orElse(null));
		entityInformation.setScheduledOperationTime(dbEntityInformation.scheduledOperationTime);
		return entityInformation;

	}
}
