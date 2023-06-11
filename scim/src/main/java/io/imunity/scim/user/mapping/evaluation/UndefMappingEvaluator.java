/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.UndefinedMapping;
import pl.edu.icm.unity.base.exceptions.EngineException;

@Component
public class UndefMappingEvaluator implements MappingEvaluator
{
	@Override
	public String getId()
	{
		return UndefinedMapping.id;
	}

	@Override
	public EvaluationResult eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry) throws EngineException
	{
		return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name).build();
	}
}
