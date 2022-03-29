/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.user;

import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SchemaWithMapping;

class SchemaFilteringSupport
{

	static SchemaWithMapping getSchemaWithFilteredAttributes(SchemaWithMapping schemaWithMapping,
			Predicate<AttributeDefinitionWithMapping> filter)
	{
		return SchemaWithMapping.builder().withName(schemaWithMapping.name).withId(schemaWithMapping.id)
				.withDescription(schemaWithMapping.description).withEnable(schemaWithMapping.enable)
				.withType(schemaWithMapping.type)
				.withAttributesWithMapping(schemaWithMapping.attributesWithMapping.stream().filter(filter)
						.map(a -> getFilteredAttribute(a, filter)).collect(Collectors.toList()))

				.build();
	}

	private static AttributeDefinitionWithMapping getFilteredAttribute(AttributeDefinitionWithMapping attributesDefinitionWithMapping,
			Predicate<AttributeDefinitionWithMapping> filter)
	{
		return AttributeDefinitionWithMapping.builder()
				.withAttributeMapping(attributesDefinitionWithMapping.attributeMapping)
				.withAttributeDefinition(AttributeDefinition
						.builder().withDescription(attributesDefinitionWithMapping.attributeDefinition.description)
						.withMultiValued(attributesDefinitionWithMapping.attributeDefinition.multiValued)
						.withName(attributesDefinitionWithMapping.attributeDefinition.name)
						.withType(attributesDefinitionWithMapping.attributeDefinition.type)
						.withSubAttributesWithMapping(
								attributesDefinitionWithMapping.attributeDefinition.subAttributesWithMapping.stream()
										.filter(filter).map(a -> getFilteredAttribute(a, filter))
										.collect(Collectors.toList()))
						.build())
				.build();
	}

}
