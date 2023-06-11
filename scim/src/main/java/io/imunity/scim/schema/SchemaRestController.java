/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema;

import java.util.Optional;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.schema.SchemaAssemblyService.SCIMSchemaAssemblyServiceFactory;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.utils.Log;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class SchemaRestController implements SCIMRestController
{
	static final String SCHEMA_LOCATION = "/Schemas";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SchemaRestController.class);
	private final ObjectMapper mapper = SCIMConstants.MAPPER;
	private final SchemaAssemblyService schemaAssemblyService;

	SchemaRestController(SchemaAssemblyService schemaAssemblyService)
	{
		this.schemaAssemblyService = schemaAssemblyService;
	}

	@Path(SCHEMA_LOCATION)
	@GET
	public Response getSchemas(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get schemas");
		return Response.ok().entity(mapper.writeValueAsString(schemaAssemblyService.getSchemasResource()))
				.contentLocation(uriInfo.getRequestUri()).build();
	}

	@Path(SCHEMA_LOCATION + "/{id}")
	@GET
	public Response getSchema(@PathParam("id") String schemaId, @Context UriInfo uriInfo)
			throws EngineException, JsonProcessingException
	{
		log.debug("Get schema with id: {}", schemaId);
		Optional<SCIMSchemaResource> schemaResource = schemaAssemblyService.getSchemaResource(new SchemaId(schemaId));
		if (schemaResource.isEmpty())
			throw new SchemaNotFoundException("Invalid schema " + schemaId);
		return Response.ok().entity(mapper.writeValueAsString(schemaResource.get()))
				.contentLocation(uriInfo.getRequestUri()).build();
	}

	@Component
	static class SCIMSchemaRestControllerFactory implements SCIMRestControllerFactory
	{
		private final SCIMSchemaAssemblyServiceFactory assemblyServiceFactory;

		@Autowired
		SCIMSchemaRestControllerFactory(SCIMSchemaAssemblyServiceFactory assemblyServiceFactory)
		{
			this.assemblyServiceFactory = assemblyServiceFactory;
		}

		@Override
		public SchemaRestController getController(SCIMEndpointDescription configuration)
		{
			return new SchemaRestController(assemblyServiceFactory.getService(configuration));
		}
	}
}
