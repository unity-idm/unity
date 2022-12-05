/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.restadm.mappers;

import io.imunity.rest.api.types.basic.RestAttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement;
import pl.edu.icm.unity.types.basic.AttributeStatement.ConflictResolution;

public class AttributeStatementMapper
{
	public static RestAttributeStatement map(AttributeStatement attributeStatement)
	{
		if (attributeStatement == null)
			return null;

		return RestAttributeStatement.builder()
				.withCondition(attributeStatement.getCondition())
				.withResolution(attributeStatement.getConflictResolution()
						.name())
				.withDynamicAttributeExpression(attributeStatement.getDynamicAttributeExpression())
				.withDynamicAttributeName(attributeStatement.getDynamicAttributeType())
				.withExtraGroupName(attributeStatement.getExtraAttributesGroup())
				.withFixedAttribute(attributeStatement.getFixedAttribute() != null
						? AttributeMapper.map(attributeStatement.getFixedAttribute())
						: null)
				.build();
	}

	public static AttributeStatement map(RestAttributeStatement attributeStatement)
	{
		if (attributeStatement == null)
			return null;

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
