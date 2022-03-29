/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import org.junit.Test;

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

		assertThat(schema.id, is("urn:ietf:params:scim:schemas:core:2.0:User"));
		assertThat(schema.name, is("User"));
		assertThat(schema.description, is("User Schema"));
		assertThat(schema.attributes.size(), is(21));

		SCIMAttributeDefinitionResource attr = schema.attributes.get(0);

		assertThat(attr.name, is("userName"));
		assertThat(attr.required, is(true));
		assertThat(attr.multiValued, is(false));
		assertThat(attr.caseExact, is(false));
		assertThat(attr.returned, is("default"));
		assertThat(attr.uniqueness, is("server"));
		assertThat(attr.mutability, is("readWrite"));

		assertThat(attr.description.startsWith("Unique identifier"), is(true));

		SCIMAttributeDefinitionResource complexAttr = schema.attributes.get(1);

		assertThat(complexAttr.name, is("name"));
		assertThat(complexAttr.required, is(false));
		assertThat(complexAttr.multiValued, is(false));
		assertThat(complexAttr.caseExact, is(false));
		assertThat(complexAttr.returned, is("default"));
		assertThat(complexAttr.uniqueness, is("none"));
		assertThat(complexAttr.mutability, is("readWrite"));
		assertThat(complexAttr.description.startsWith("The components of the user's real name."), is(true));
		assertThat(complexAttr.subAttributes.size(), is(6));
		assertThat(complexAttr.subAttributes.stream().map(a -> a.name).collect(Collectors.toSet()),
				hasItems("formatted", "familyName", "givenName", "middleName", "honorificPrefix", "honorificSuffix"));
	}
}
