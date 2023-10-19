/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user.mapping.evaluation;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.UriBuilder;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.ReferenceAttributeMapping;
import io.imunity.scim.config.ReferenceAttributeMapping.ReferenceType;
import io.imunity.scim.group.GroupRestController;
import io.imunity.scim.user.UserRestController;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;

@Component
class ReferenceMappingEvaluator implements MappingEvaluator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, ReferenceMappingEvaluator.class);

	private final MVELEvaluator mvelEvaluator;
	private final DataArrayResolver dataArrayResolver;

	ReferenceMappingEvaluator(MVELEvaluator mvelEvaluator, DataArrayResolver dataArrayResolver)
	{

		this.mvelEvaluator = mvelEvaluator;
		this.dataArrayResolver = dataArrayResolver;
	}

	@Override
	public String getId()
	{
		return ReferenceAttributeMapping.id;
	}

	@Override
	public EvaluationResult eval(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry) throws EngineException
	{

		log.debug("Eval reference mapping for attribute {}", attributeDefinitionWithMapping.attributeDefinition.name);

		ReferenceAttributeMapping mapping = (ReferenceAttributeMapping) attributeDefinitionWithMapping.attributeMapping;
		return attributeDefinitionWithMapping.attributeDefinition.multiValued
				? evalMulti(attributeDefinitionWithMapping, context, registry, mapping)
				: evalSingle(attributeDefinitionWithMapping, context, registry, mapping);

	}

	private EvaluationResult evalMulti(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry, ReferenceAttributeMapping mapping)
			throws EngineException
	{
		if (attributeDefinitionWithMapping.attributeMapping.getDataArray().isEmpty())
		{
			return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name)
					.build();
		}
		List<Object> ret = new ArrayList<>();
		for (Object arrayObj : dataArrayResolver.resolve(mapping.getDataArray().get(), context))
		{
			ret.add(resolveReference(mapping.type,
					mvelEvaluator.evalMVEL(mapping.expression,
							EvaluatorContext.builder().withUser(context.user).withArrayObj(arrayObj)
									.withScimEndpointDescription(context.scimEndpointDescription)
									.withGroupProvider(context.groupProvider).build()),
					context));
		}
		return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name)
				.withValue(Optional.ofNullable(ret.isEmpty() ? null : ret)).build();

	}

	private EvaluationResult evalSingle(AttributeDefinitionWithMapping attributeDefinitionWithMapping,
			EvaluatorContext context, MappingEvaluatorRegistry registry, ReferenceAttributeMapping mapping)
			throws EngineException
	{

		return EvaluationResult.builder().withAttributeName(attributeDefinitionWithMapping.attributeDefinition.name)
				.withValue(Optional.ofNullable(
						resolveReference(mapping.type, mvelEvaluator.evalMVEL(mapping.expression, context), context)))
				.build();

	}

	private Object resolveReference(ReferenceType type, Object value, EvaluatorContext context)
	{
		if (value == null)
			return null;

		switch (type)
		{
		case GROUP:
			return getGroupLocation(value.toString(), context);
		case USER:
			return getUserLocation(value.toString(), context);
		default:
			return UriBuilder.fromUri(value.toString()).build();
		}
	}

	private URI getGroupLocation(String group, EvaluatorContext context)
	{
		return UriBuilder.fromUri(context.scimEndpointDescription.baseLocation).path(GroupRestController.GROUP_LOCATION)
				.path(URLEncoder.encode(group, StandardCharsets.UTF_8)).build();
	}

	private URI getUserLocation(String user, EvaluatorContext context)
	{
		return UriBuilder.fromUri(context.scimEndpointDescription.baseLocation).path(UserRestController.USER_LOCATION)
				.path(URLEncoder.encode(user, StandardCharsets.UTF_8)).build();
	}
}