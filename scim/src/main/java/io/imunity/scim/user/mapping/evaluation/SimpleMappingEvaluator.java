/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SimpleAttributeMapping;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Component
class SimpleMappingEvaluator implements MappingEvaluator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SimpleMappingEvaluator.class);

	private final DataArrayResolver dataArrayResolver;
	private final UnityToSCIMDataConverter targetDataConverter;
	private final MVELEvaluator mvelEvaluator;

	SimpleMappingEvaluator(DataArrayResolver dataArrayResolver, UnityToSCIMDataConverter targetDataConverter,
			MVELEvaluator mvelEvaluator)
	{
		this.dataArrayResolver = dataArrayResolver;
		this.targetDataConverter = targetDataConverter;
		this.mvelEvaluator = mvelEvaluator;
	}

	@Override
	public String getId()
	{
		return SimpleAttributeMapping.id;
	}

	@Override
	public Map<String, Object> eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry) throws EngineException
	{
		log.debug("Eval simple mapping for attribute {}", attributeDefinitionWithMapping.attributeDefinition.name);

		SimpleAttributeMapping mapping = (SimpleAttributeMapping) attributeDefinitionWithMapping.attributeMapping;
		return attributeDefinitionWithMapping.attributeDefinition.multiValued
				? evalMulti(attributeDefinitionWithMapping, context, registry, mapping)
				: evalSingle(attributeDefinitionWithMapping, context, registry, mapping);
	}

	private Map<String, Object> evalSingle(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry, SimpleAttributeMapping mapping)
			throws EngineException
	{

		Optional<Object> value = Optional.empty();
		switch (mapping.dataValue.type)
		{
		case MVEL:
			value = Optional.ofNullable(mvelEvaluator.evalMVEL(mapping.dataValue.value.get(), context));
			break;
		case ATTRIBUTE:
			value = targetDataConverter.convertUserAttributeToType(context.user, mapping.dataValue.value.get(),
					attributeDefinitionWithMapping.attributeDefinition.type);
			break;
		case IDENTITY:
			value = targetDataConverter.convertUserIdentityToType(context.user, mapping.dataValue.value.get(),
					attributeDefinitionWithMapping.attributeDefinition.type);
			break;
		default:
			throw new UnsupportedOperationException(
					mapping.dataValue.type + " dataValue type is not supported by single simple mapping");
		}

		return Collections.singletonMap(attributeDefinitionWithMapping.attributeDefinition.name, value.isEmpty() ? null : value.get());

	}

	private Map<String, Object> evalMulti(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry, SimpleAttributeMapping mapping)
			throws EngineException
	{
		if (attributeDefinitionWithMapping.attributeMapping.getDataArray().isEmpty())
			return Collections.emptyMap();
		List<Object> evalRet = new ArrayList<>();
		for (Object arrayObj : dataArrayResolver.resolve(mapping.getDataArray().get(), context))
		{
			switch (mapping.dataValue.type)
			{
			case MVEL:
				evalRet.add(mvelEvaluator.evalMVEL(mapping.dataValue.value.get(),
						EvaluatorContext.builder().withUser(context.user).withArrayObj(arrayObj)
								.withScimEndpointDescription(context.scimEndpointDescription)
								.withGroupProvider(context.groupProvider).build()));
				break;
			case ARRAY:
				evalRet.add(targetDataConverter.convertToType(arrayObj,
						attributeDefinitionWithMapping.attributeDefinition.type));
				break;
			default:
				throw new UnsupportedOperationException(
						mapping.dataValue.type + " dataValue type is not supported in single simple mapping");
			}
		}
		return Map.of(attributeDefinitionWithMapping.attributeDefinition.name, evalRet);
	}
}