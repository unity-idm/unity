/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.impl.groups;

import java.util.Optional;

import pl.edu.icm.unity.base.attribute.AttributeStatement;
import pl.edu.icm.unity.base.attribute.AttributeStatement.ConflictResolution;
import pl.edu.icm.unity.store.impl.attribute.AttributeMapper;

class AttributeStatementMapper
{
	static DBAttributeStatement map(AttributeStatement attributeStatement)
	{
		return DBAttributeStatement.builder()
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

	static AttributeStatement map(DBAttributeStatement attributeStatement)
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
