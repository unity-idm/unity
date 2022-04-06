/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	public EvaluationResult eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry mappingEvaluatorRegistry) throws EngineException
	{

		log.debug("Eval complex mapping for attribute {}", attributeDefinitionWithMapping.attributeDefinition.name);

		return attributeDefinitionWithMapping.attributeDefinition.multiValued
				? evalMulti(attributeDefinitionWithMapping, context, mappingEvaluatorRegistry)
				: evalSingle(attributeDefinitionWithMapping, context, mappingEvaluatorRegistry);

	}

	private EvaluationResult evalMulti(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry mappingEvaluatorRegistry) throws EngineException
	{
		List<Object> retArray = new ArrayList<>();
		if (attributeDefinitionWithMapping.attributeMapping.getDataArray().isEmpty())
			return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name)
					.build();

		for (Object arrayObj : dataArrayResolver
				.resolve(attributeDefinitionWithMapping.attributeMapping.getDataArray().get(), context))
		{
			Map<String, Object> subAtributeEvalRet = new HashMap<>();
			for (AttributeDefinitionWithMapping subAttr : attributeDefinitionWithMapping.attributeDefinition.subAttributesWithMapping)
			{
				EvaluationResult sResult = mappingEvaluatorRegistry.getByName(subAttr.attributeMapping.getEvaluatorId())
						.eval(subAttr,
								EvaluatorContext.builder().withUser(context.user).withArrayObj(arrayObj)
										.withScimEndpointDescription(context.scimEndpointDescription)
										.withGroupProvider(context.groupProvider).build(),
								mappingEvaluatorRegistry);
				if (sResult.value.isPresent())
				{
					subAtributeEvalRet.put(sResult.attributeName, sResult.value.get());
				}
			}
			retArray.add(subAtributeEvalRet);
		}
		return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name)
				.withValue(Optional.ofNullable(retArray.isEmpty() ? null : retArray)).build();

	}

	private EvaluationResult evalSingle(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry mappingEvaluatorRegistry) throws EngineException
	{
		Map<String, Object> ret = new HashMap<>();
		for (AttributeDefinitionWithMapping subAttr : attributeDefinitionWithMapping.attributeDefinition.subAttributesWithMapping)
		{
			EvaluationResult sResult = mappingEvaluatorRegistry.getByName(subAttr.attributeMapping.getEvaluatorId())
					.eval(subAttr, context, mappingEvaluatorRegistry);
			if (sResult.value.isPresent())
			{
				ret.put(sResult.attributeName, sResult.value.get());
			}
		}
		return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name)
				.withValue(Optional.ofNullable(ret.isEmpty() ? null : ret)).build();
	}
}