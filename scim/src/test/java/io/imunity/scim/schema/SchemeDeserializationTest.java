/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;

public class SchemeDeserializationTest
{
	private final ObjectMapper mapper = SCIMConstants.MAPPER;

	@Test
	public void shouldDesarializeStandardUserScheme() throws StreamReadException, DatabindException, IOException
	{

		SCIMSchemaResource schema = mapper.readValue(new File("src/test/resources/UserScheme.json"), SCIMSchemaResource.class);

		assertThat(schema.id).isEqualTo("urn:ietf:params:scim:schemas:core:2.0:User");
		assertThat(schema.name).isEqualTo("User");
		assertThat(schema.description).isEqualTo("User Schema");
		assertThat(schema.attributes.size()).isEqualTo(21);

		SCIMAttributeDefinitionResource attr = schema.attributes.get(0);

		assertThat(attr.name).isEqualTo("userName");
		assertThat(attr.required).isEqualTo(true);
		assertThat(attr.multiValued).isEqualTo(false);
		assertThat(attr.caseExact).isEqualTo(false);
		assertThat(attr.returned).isEqualTo("default");
		assertThat(attr.uniqueness).isEqualTo("server");
		assertThat(attr.mutability).isEqualTo("readWrite");

		assertThat(attr.description.startsWith("Unique identifier")).isEqualTo(true);

		SCIMAttributeDefinitionResource complexAttr = schema.attributes.get(1);

		assertThat(complexAttr.name).isEqualTo("name");
		assertThat(complexAttr.required).isEqualTo(false);
		assertThat(complexAttr.multiValued).isEqualTo(false);
		assertThat(complexAttr.caseExact).isEqualTo(false);
		assertThat(complexAttr.returned).isEqualTo("default");
		assertThat(complexAttr.uniqueness).isEqualTo("none");
		assertThat(complexAttr.mutability).isEqualTo("readWrite");
		assertThat(complexAttr.description.startsWith("The components of the user's real name.")).isEqualTo(true);
		assertThat(complexAttr.subAttributes.size()).isEqualTo(6);
		assertThat(complexAttr.subAttributes.stream().map(a -> a.name).collect(Collectors.toSet())).
				contains("formatted", "familyName", "givenName", "middleName", "honorificPrefix", "honorificSuffix");
	}
}
