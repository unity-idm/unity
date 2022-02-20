/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.AttributeMapping;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;

public class SchemaResourceDeserialaizer
{
	public static SchemaWithMapping deserializeUserSchemaFromFile(File file)
			throws StreamReadException, DatabindException, IOException
	{
		SCIMSchemaResource schema = SCIMConstants.MAPPER.readValue(file, SCIMSchemaResource.class);
		return SchemaWithMapping.builder().withId(schema.id).withType(SchemaType.USER)
				.withDescription(schema.description).withName(schema.name).withAttributesWithMapping(
						schema.attributes.stream().map(a -> mapAttribute(a)).collect(Collectors.toList()))
				.build();
	}

	private static AttributeDefinitionWithMapping mapAttribute(SCIMAttributeDefinitionResource attrDefResource)
	{
		return AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withDescription(attrDefResource.description)
						.withName(attrDefResource.name).withType(SCIMAttributeType.fromName(attrDefResource.type))
						.withMultiValued(attrDefResource.multiValued)
						.withSubAttributesWithMapping(attrDefResource.subAttributes.stream().map(sa -> mapAttribute(sa))
								.collect(Collectors.toList()))
						.build())
				.withAttributeMapping(AttributeMapping.builder().build()).build();
	}
}
