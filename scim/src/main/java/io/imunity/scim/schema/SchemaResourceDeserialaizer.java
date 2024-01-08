/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonNode;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;
import pl.edu.icm.unity.exceptions.EngineException;

public class SchemaResourceDeserialaizer
{
	public static SchemaWithMapping deserializeUserSchemaFromFile(File file)
			throws StreamReadException, DatabindException, IOException, EngineException
	{
		JsonNode node = SCIMConstants.MAPPER.readTree(file);
		String type = node.has("type") ? node.get("type")
				.asText() : null;
		return type == null ? mapSCIMSchema(file) : mapSchemaWithMapping(type, file);
	}

	private static SchemaWithMapping mapSCIMSchema(File file) throws StreamReadException, DatabindException, IOException
	{
		SCIMSchemaResource schema = SCIMConstants.MAPPER.readValue(file, SCIMSchemaResource.class);
		return SchemaWithMapping.builder()
				.withId(schema.id)
				.withType(SchemaType.USER)
				.withDescription(schema.description)
				.withName(schema.name)
				.withAttributesWithMapping(mapAttributes(schema.attributes))
				.build();
	}

	private static SchemaWithMapping mapSchemaWithMapping(String type, File file)
			throws EngineException, StreamReadException, DatabindException, IOException
	{
		if (!type.equals(SchemaType.USER.name()))
		{
			throw new EngineException("Can not import schema with type: " + type + ". Only USER type is supported");
		}
		return SCIMConstants.MAPPER.readValue(file, SchemaWithMapping.class);
	}
	
	private static AttributeDefinitionWithMapping mapAttribute(SCIMAttributeDefinitionResource attrDefResource)
	{
		return AttributeDefinitionWithMapping.builder()
				.withAttributeDefinition(AttributeDefinition.builder().withDescription(attrDefResource.description)
						.withName(attrDefResource.name).withType(SCIMAttributeType.fromName(attrDefResource.type))
						.withMultiValued(attrDefResource.multiValued)
						.withSubAttributesWithMapping(mapAttributes(attrDefResource.subAttributes)).build())
				.withAttributeMapping(null).build();
	}

	private static List<AttributeDefinitionWithMapping> mapAttributes(List<SCIMAttributeDefinitionResource> attrs)
	{
		List<AttributeDefinitionWithMapping> subAttrMapping = new ArrayList<>();
		for (SCIMAttributeDefinitionResource sa : attrs)
		{
			subAttrMapping.add(mapAttribute(sa));
		}
		return subAttrMapping;
	}
}
