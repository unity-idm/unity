/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import pl.edu.icm.unity.base.exceptions.EngineException;

public interface MappingEvaluator
{
	String getId();

	EvaluationResult eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping, EvaluatorContext context,
			MappingEvaluatorRegistry registry) throws EngineException;
}
