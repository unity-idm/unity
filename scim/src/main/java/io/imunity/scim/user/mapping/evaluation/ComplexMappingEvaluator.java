/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.ComplexAttributeMapping;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
class ComplexMappingEvaluator implements MappingEvaluator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, ComplexMappingEvaluator.class);

	private final DataArrayResolver dataArrayResolver;

	ComplexMappingEvaluator(DataArrayResolver dataArrayResolver)
	{
		this.dataArrayResolver = dataArrayResolver;
	}

	@Override
	public String getId()
	{
		return ComplexAttributeMapping.id;
	}

	@Override
	public Map<String, Object> eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry mappingEvaluatorRegistry) throws EngineException
	{

		log.debug("Eval complex mapping for attribute {}", attributeDefinitionWithMapping.attributeDefinition.name);

		return attributeDefinitionWithMapping.attributeDefinition.multiValued
				? evalMulti(attributeDefinitionWithMapping, context, mappingEvaluatorRegistry)
				: evalSingle(attributeDefinitionWithMapping, context, mappingEvaluatorRegistry);

	}

	private Map<String, Object> evalMulti(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry mappingEvaluatorRegistry) throws EngineException
	{
		List<Object> retArray = new ArrayList<>();
		if (attributeDefinitionWithMapping.attributeMapping.getDataArray().isEmpty())
			return Collections.emptyMap();
		
		for (Object arrayObj : dataArrayResolver
				.resolve(attributeDefinitionWithMapping.attributeMapping.getDataArray().get(), context))
		{
			Map<String, Object> subAtributeEvalRet = new HashMap<>();
			for (AttributeDefinitionWithMapping subAttr : attributeDefinitionWithMapping.attributeDefinition.subAttributesWithMapping)
			{
				subAtributeEvalRet.putAll(
						mappingEvaluatorRegistry.getByName(subAttr.attributeMapping.getEvaluatorId()).eval(subAttr,
								EvaluatorContext.builder().withUser(context.user).withArrayObj(arrayObj)
										.withScimEndpointDescription(context.scimEndpointDescription)
										.withGroupProvider(context.groupProvider).build(),
								mappingEvaluatorRegistry));
			}
			retArray.add(subAtributeEvalRet);
		}
		return Map.of(attributeDefinitionWithMapping.attributeDefinition.name, retArray);

	}

	private Map<String, Object> evalSingle(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry mappingEvaluatorRegistry) throws EngineException
	{
		Map<String, Object> ret = new HashMap<>();
		for (AttributeDefinitionWithMapping subAttr : attributeDefinitionWithMapping.attributeDefinition.subAttributesWithMapping)
		{
			ret.putAll(mappingEvaluatorRegistry.getByName(subAttr.attributeMapping.getEvaluatorId()).eval(subAttr,
					context, mappingEvaluatorRegistry));
		}
		return Map.of(attributeDefinitionWithMapping.attributeDefinition.name, ret);
	}
}