/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.NotDefinedMapping;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
public class NotDefMappingEvaluator implements MappingEvaluator
{
	@Override
	public String getId()
	{
		return NotDefinedMapping.id;
	}

	@Override
	public EvaluationResult eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry) throws EngineException
	{
		return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name).build();
	}
}
