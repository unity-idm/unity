/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import io.imunity.scim.common.ListResponse;
import io.imunity.scim.config.AttributeDefinition;
import io.imunity.scim.config.AttributeDefinitionWithMapping;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.config.SchemaType;
import io.imunity.scim.config.SchemaWithMapping;

@RunWith(MockitoJUnitRunner.class)
public class SchemeAssemblyServiceTest
{
	private SchemaAssemblyService schemaAssemblyService;

	@Before
	public void init()
	{
		SCIMEndpointDescription configuration = new SCIMEndpointDescription(URI.create("https://localhost:2443/scim"),
				"/scim", List.of("/scim"),
				List.of(DefaultSchemaProvider.getBasicGroupSchema(), DefaultSchemaProvider.getBasicUserSchema(),
						SchemaWithMapping.builder().withId("urn:ietf:params:scim:schemas:NotEnabled")
								.withType(SchemaType.USER).withName("NotEnabled").withDescription("NotEnabled")
								.withEnable(false).build(),
						SchemaWithMapping.builder().withId("urn:ietf:params:scim:schemas:UserExt")
								.withType(SchemaType.USER).withName("UserExt").withDescription("UserExtDesc")
								.withEnable(true)
								.withAttributesWithMapping(List.of(AttributeDefinitionWithMapping.builder()
										.withAttributeDefinition(AttributeDefinition.builder().withName("userAttr1")
												.withMultiValued(false).withType(SCIMAttributeType.STRING)
												.withDescription("userAttrDesc1").build())
										.withAttributeMapping(null).build(),
										AttributeDefinitionWithMapping.builder()
												.withAttributeDefinition(AttributeDefinition.builder()
														.withName("userAttr2").withMultiValued(true)
														.withType(SCIMAttributeType.COMPLEX)
														.withDescription("userAttrDesc2")
														.withSubAttributesWithMapping(
																List.of(AttributeDefinitionWithMapping.builder()
																		.withAttributeDefinition(AttributeDefinition
																				.builder().withName("subUserAttr1")
																				.withMultiValued(false)
																				.withType(SCIMAttributeType.STRING)
																				.withDescription("subUserAttrDesc1")
																				.build())
																		.withAttributeMapping(null).build()))
														.build())
												.withAttributeMapping(null).build()))

								.build()), Collections.emptyList(), Collections.emptyList());

		schemaAssemblyService = new SchemaAssemblyService(configuration);
	}

	@Test
	public void shouldReturnOnlyEnabledSchemas()
	{
		ListResponse<SCIMSchemaResource> schemaResources = schemaAssemblyService.getSchemasResource();

		assertThat(schemaResources.totalResults, is(3));
		assertThat(schemaResources.resources.stream().map(s -> s.id).collect(Collectors.toSet()),
				hasItems(DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID, DefaultSchemaProvider.DEFAULT_GROUP_SCHEMA_ID,
						"urn:ietf:params:scim:schemas:UserExt"));
	}

	@Test
	public void shouldReturnSingleSchema()
	{
		Optional<SCIMSchemaResource> schemaResource = schemaAssemblyService
				.getSchemaResource(new SchemaId(DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID));
		assertThat(schemaResource.isPresent(), is(true));
		assertThat(schemaResource.get().id, is(DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID));
	}

	@Test
	public void shouldReturnSchemaWithFixedAttributesCharacteristics()
	{
		Optional<SCIMSchemaResource> schemaResource = schemaAssemblyService
				.getSchemaResource(new SchemaId(DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID));
		assertThat(schemaResource.isPresent(), is(true));
		assertThat(schemaResource.get().id, is(DefaultSchemaProvider.DEFAULT_USER_SCHEMA_ID));
		for (SCIMAttributeDefinitionResource attr : schemaResource.get().attributes)
		{
			assertThat(attr.required, is(false));
			assertThat(attr.caseExact, is(true));
			assertThat(attr.mutability, is(SCIMAttributeMutability.IMMUTABLE.getName()));
			assertThat(attr.returned, is(SCIMAttributeReturned.DEFAULT.getName()));
			assertThat(attr.uniqueness, is(SCIMAttributeUniqueness.NONE.getName()));
		}
	}

	@Test
	public void shouldReturnSignleSchema() throws MalformedURLException
	{
		Optional<SCIMSchemaResource> schemaResource = schemaAssemblyService
				.getSchemaResource(new SchemaId("urn:ietf:params:scim:schemas:UserExt"));
		assertThat(schemaResource.isPresent(), is(true));
		assertThat(schemaResource.get().id, is("urn:ietf:params:scim:schemas:UserExt"));
		assertThat(schemaResource.get().description, is("UserExtDesc"));
		assertThat(schemaResource.get().description, is("UserExtDesc"));
		assertThat(schemaResource.get().meta.resourceType.toString(), is("Schema"));
		assertThat(schemaResource.get().meta.location.toURL().toExternalForm(),
				is("https://localhost:2443/scim/Schemas/urn%3Aietf%3Aparams%3Ascim%3Aschemas%3AUserExt"));

		assertThat(schemaResource.get().attributes,
				hasItems(
						SCIMAttributeDefinitionResource.builder().withName("userAttr1").withDescription("userAttrDesc1")
								.withType("string").withMultiValued(false).withCaseExact(true).withRequired(false)
								.withMutability(SCIMAttributeMutability.IMMUTABLE.getName())
								.withReturned(SCIMAttributeReturned.DEFAULT.getName())
								.withUniqueness(SCIMAttributeUniqueness.NONE.getName()).build(),
						SCIMAttributeDefinitionResource.builder().withName("userAttr2").withDescription("userAttrDesc2")
								.withType("complex").withMultiValued(true).withCaseExact(true).withRequired(false)
								.withMutability(SCIMAttributeMutability.IMMUTABLE.getName())
								.withReturned(SCIMAttributeReturned.DEFAULT.getName())
								.withUniqueness(SCIMAttributeUniqueness.NONE.getName())
								.withSubAttributes(List.of(SCIMAttributeDefinitionResource.builder()
										.withName("subUserAttr1").withDescription("subUserAttrDesc1").withType("string")
										.withMultiValued(false).withCaseExact(true).withRequired(false)
										.withMutability(SCIMAttributeMutability.IMMUTABLE.getName())
										.withReturned(SCIMAttributeReturned.DEFAULT.getName())
										.withUniqueness(SCIMAttributeUniqueness.NONE.getName()).build()))
								.build()));
	}

	@Test
	public void shouldReturnEmptyWhenUnknownId() throws MalformedURLException
	{
		Optional<SCIMSchemaResource> schemaResource = schemaAssemblyService.getSchemaResource(new SchemaId("unknown"));
		assertThat(schemaResource.isEmpty(), is(true));
	}
}
