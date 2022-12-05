/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;

import io.imunity.rest.api.types.basic.RestEntityInformation;
import pl.edu.icm.unity.types.basic.EntityInformation;
import pl.edu.icm.unity.types.basic.EntityScheduledOperation;
import pl.edu.icm.unity.types.basic.EntityState;

public class EntityInformationMapper
{
	static RestEntityInformation map(EntityInformation entityInformation)
	{
		return RestEntityInformation.builder()
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

	static EntityInformation map(RestEntityInformation restEntityInformation)
	{
		EntityInformation entityInformation = new EntityInformation(restEntityInformation.entityId);
		entityInformation.setEntityState(Optional.ofNullable(restEntityInformation.state)
				.map(s -> EntityState.valueOf(s))
				.orElse(null));
		entityInformation.setRemovalByUserTime(restEntityInformation.removalByUserTime);
		entityInformation.setScheduledOperation(Optional.ofNullable(restEntityInformation.scheduledOperation)
				.map(s -> EntityScheduledOperation.valueOf(s))
				.orElse(null));
		entityInformation.setScheduledOperationTime(restEntityInformation.scheduledOperationTime);
		return entityInformation;

	}
}
