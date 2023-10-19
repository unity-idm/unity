/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.common.ListResponse;
import pl.edu.icm.unity.base.exceptions.EngineException;

@ExtendWith(MockitoExtension.class)
public class SchemaRestControllerTest
{
	@Mock
	private SchemaAssemblyService schemaAssemblyService;

	@Mock
	private UriInfo uriInfo;

	private SchemaRestController controller;

	@BeforeEach
	public void init()
	{
		controller = new SchemaRestController(schemaAssemblyService);
	}

	@Test
	public void shouldThrowExceptionWhenUknownSchema()
	{
		when(schemaAssemblyService.getSchemaResource(new SchemaId("unknown"))).thenReturn(Optional.empty());
		Throwable error = Assertions.catchThrowable(() -> controller.getSchema("unknown", null));
		Assertions.assertThat(error).isInstanceOf(SchemaNotFoundException.class);
	}

	@Test
	public void shouldReturnSingleSchema() throws JsonProcessingException, EngineException
	{
		SCIMSchemaResource schema = SCIMSchemaResource.builder().withId("id").withName("name").build();

		when(schemaAssemblyService.getSchemaResource(new SchemaId("id"))).thenReturn(Optional.of(schema));
		Response resp = controller.getSchema("id", uriInfo);
		assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		assertThat(resp.getEntity()).isEqualTo(SCIMConstants.MAPPER.writeValueAsString(schema));
	}

	@Test
	public void shouldReturnSchemas() throws JsonProcessingException, EngineException
	{
		SCIMSchemaResource schema1 = SCIMSchemaResource.builder().withId("id1").withName("name1").build();
		SCIMSchemaResource schema2 = SCIMSchemaResource.builder().withId("id2").withName("name2").build();
		ListResponse<SCIMSchemaResource> schemas = ListResponse.<SCIMSchemaResource>builder()
				.withResources(List.of(schema1, schema2)).withTotalResults(2).build();
		when(schemaAssemblyService.getSchemasResource()).thenReturn(schemas);
		Response resp = controller.getSchemas(uriInfo);
		assertThat(resp.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
		assertThat(resp.getEntity()).isEqualTo(SCIMConstants.MAPPER.writeValueAsString(schemas));
	}

}
