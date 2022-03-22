/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import io.imunity.scim.user.User;
import pl.edu.icm.unity.engine.api.mvel.CachingMVELGroupProvider;
import pl.edu.icm.unity.exceptions.EngineException;

public class UserSchemaEvaluator
{
	private final MappingEvaluatorRegistry mappingEvaluatorRegistry;
	private final SCIMEndpointDescription configuration;

	UserSchemaEvaluator(SCIMEndpointDescription configuration, MappingEvaluatorRegistry mappingEvaluatorRegistry)
	{
		this.mappingEvaluatorRegistry = mappingEvaluatorRegistry;
		this.configuration = configuration;
	}

	public Map<String, Object> evalUserSchema(User user, SchemaWithMapping basicSchema, List<SchemaWithMapping> schemas,
			CachingMVELGroupProvider cachingMVELGroupProvider) throws EngineException
	{
		Map<String, Object> attributeEvaluationResult = new LinkedHashMap<>();
		attributeEvaluationResult.putAll(evalSchema(basicSchema, user, cachingMVELGroupProvider));

		for (SchemaWithMapping schema : schemas.stream().filter(s -> s.type.equals(SchemaType.USER) && s.enable)
				.collect(Collectors.toList()))
		{
			attributeEvaluationResult.put(schema.id, evalSchema(schema, user, cachingMVELGroupProvider));
		}
		return attributeEvaluationResult;
	}

	private Map<String, Object> evalSchema(SchemaWithMapping schema, User user,
			CachingMVELGroupProvider cachingMVELGroupProvider) throws EngineException
	{
		Map<String, Object> ret = new HashMap<>();
		for (AttributeDefinitionWithMapping attributeWithMapping : schema.attributesWithMapping)
		{
			MappingEvaluator evaluator = mappingEvaluatorRegistry
					.getByName(attributeWithMapping.attributeMapping.getEvaluatorId());
			EvaluationResult eval = evaluator.eval(attributeWithMapping, EvaluatorContext.builder().withUser(user)
					.withGroupProvider(cachingMVELGroupProvider).withScimEndpointDescription(configuration).build(),
					mappingEvaluatorRegistry);
			if (eval.value.isPresent())
			{
				ret.put(eval.attributeName, eval.value.get());
			}
		}
		return ret;
	}

	@Component
	public static class UserSchemaEvaluatorFactory
	{
		private MappingEvaluatorRegistry mappingEvaluatorRegistry;

		public UserSchemaEvaluatorFactory(MappingEvaluatorRegistry mappingEvaluatorRegistry)
		{
			this.mappingEvaluatorRegistry = mappingEvaluatorRegistry;
		}

		public UserSchemaEvaluator getService(SCIMEndpointDescription configuration)
		{
			return new UserSchemaEvaluator(configuration, mappingEvaluatorRegistry);
		}
	}
}
