/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.Map;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import pl.edu.icm.unity.exceptions.EngineException;

public interface MappingEvaluator
{
	String getId();

	Map<String, Object> eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping, EvaluatorContext context,
			MappingEvaluatorRegistry registry) throws EngineException;
}
