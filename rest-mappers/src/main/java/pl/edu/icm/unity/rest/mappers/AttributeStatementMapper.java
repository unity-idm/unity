/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.mappers;

import java.util.Optional;

import io.imunity.rest.api.types.basic.RestAttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;

public class AttributeStatementMapper
{
	public static RestAttributeStatement map(AttributeStatement attributeStatement)
	{
		return RestAttributeStatement.builder()
				.withCondition(attributeStatement.getCondition())
				.withResolution(attributeStatement.getConflictResolution()
						.name())
				.withDynamicAttributeExpression(attributeStatement.getDynamicAttributeExpression())
				.withDynamicAttributeName(attributeStatement.getDynamicAttributeType())
				.withExtraGroupName(attributeStatement.getExtraAttributesGroup())
				.withFixedAttribute(Optional.ofNullable(attributeStatement.getFixedAttribute())
						.map(AttributeMapper::map)
						.orElse(null))
				.build();
	}

	public static AttributeStatement map(RestAttributeStatement attributeStatement)
	{
		if (attributeStatement.fixedAttribute != null)
		{
			return new AttributeStatement(attributeStatement.condition, attributeStatement.extraGroupName,
					ConflictResolution.valueOf(attributeStatement.resolution),
					AttributeMapper.map(attributeStatement.fixedAttribute));
		} else
		{
			return new AttributeStatement(attributeStatement.condition, attributeStatement.extraGroupName,
					ConflictResolution.valueOf(attributeStatement.resolution), attributeStatement.dynamicAttributeName,
					attributeStatement.dynamicAttributeExpression);
		}
	}
}
