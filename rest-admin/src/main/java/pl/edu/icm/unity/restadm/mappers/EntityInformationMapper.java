/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import java.util.Optional;

import io.imunity.rest.api.types.basic.RestEntityInformation;
import pl.edu.icm.unity.types.basic.EntityInformation;

public class EntityInformationMapper
{
	static RestEntityInformation map(EntityInformation entityInformation)
	{
		return RestEntityInformation.builder().withId(entityInformation.getId())
				.withEntityState(
						Optional.ofNullable(entityInformation.getEntityState()).map(s -> s.name()).orElse(null))
				.withRemovalByUserTime(entityInformation.getRemovalByUserTime())
				.withScheduledOperationTime(entityInformation.getScheduledOperationTime())
				.withScheduledOperation(
						Optional.ofNullable(entityInformation.getScheduledOperation()).map(s -> s.name()).orElse(null))
				.build();

	}
}
