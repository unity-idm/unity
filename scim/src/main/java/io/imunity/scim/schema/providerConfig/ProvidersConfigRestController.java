/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.schema.providerConfig;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.imunity.scim.SCIMConstants;
import io.imunity.scim.SCIMEndpoint;
import io.imunity.scim.SCIMRestController;
import io.imunity.scim.SCIMRestControllerFactory;
import io.imunity.scim.common.Meta;
import io.imunity.scim.common.ResourceType;
import io.imunity.scim.config.SCIMEndpointDescription;
import io.imunity.scim.schema.providerConfig.SCIMProviderConfigResource.AuthenticationSchema;
import io.imunity.scim.schema.providerConfig.SCIMProviderConfigResource.Supported;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;

@Produces(MediaType.APPLICATION_JSON)
@Path(SCIMEndpoint.PATH)
public class ProvidersConfigRestController implements SCIMRestController
{
	static final String PROVIDER_CONFIG_LOCATION = "/ServiceProviderConfig";
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, ProvidersConfigRestController.class);
	private final ObjectMapper mapper = SCIMConstants.MAPPER;
	private final SCIMEndpointDescription configuration;
	private final AuthenticationSchemesProvider provider;

	public ProvidersConfigRestController(SCIMEndpointDescription configuration, AuthenticationSchemesProvider provider)
	{
		this.configuration = configuration;
		this.provider = provider;
	}

	@Path(PROVIDER_CONFIG_LOCATION)
	@GET
	public Response getConfigs(@Context UriInfo uriInfo) throws EngineException, JsonProcessingException
	{
		log.debug("Get providers config");
		return Response.ok().entity(mapper.writeValueAsString(SCIMProviderConfigResource.builder()
				.withPatch(Supported.builder().build())
				.withBulk(Supported.builder().build()).withFilter(Supported.builder().build())
				.withEtag(Supported.builder().build()).withChangePassword(Supported.builder().build())
				.withAuthenticationSchemes(getAuthenticationSchemes())
				.withMeta(Meta.builder()
						.withLocation(
								UriBuilder.fromUri(configuration.baseLocation).path(PROVIDER_CONFIG_LOCATION).build())
						.withCreated(Instant.now()).withResourceType(ResourceType.SERVICE_PROVIDER_CONFIG.getName())
						.build())
				.build())).contentLocation(uriInfo.getRequestUri()).build();

	}

	private List<AuthenticationSchema> getAuthenticationSchemes()
	{
		return provider.getAuthenticationSchemes(configuration.authenticationOptions).stream().collect(Collectors.toList());

	}

	@Component
	static class SCIMProvidersConfigRestControllerFactory implements SCIMRestControllerFactory
	{
		private final AuthenticationSchemesProvider provider;

		public SCIMProvidersConfigRestControllerFactory(AuthenticationSchemesProvider provider)
		{
			this.provider = provider;
		}

		@Override
		public SCIMRestController getController(SCIMEndpointDescription configuration)
		{
			return new ProvidersConfigRestController(configuration, provider);
		}
	}
}
