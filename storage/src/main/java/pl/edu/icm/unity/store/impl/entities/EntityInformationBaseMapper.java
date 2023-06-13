/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.entities;

import java.util.Optional;

import pl.edu.icm.unity.base.identity.EntityInformation;
import pl.edu.icm.unity.base.identity.EntityScheduledOperation;
import pl.edu.icm.unity.base.identity.EntityState;

class EntityInformationBaseMapper
{
	static DBEntityInformationBase map(EntityInformation entityInformation)
	{
		return DBEntityInformationBase.builder()
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

	static EntityInformation map(DBEntityInformationBase dbEntityInformation, long entityId)
	{
		EntityInformation entityInformation = new EntityInformation(entityId);
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
