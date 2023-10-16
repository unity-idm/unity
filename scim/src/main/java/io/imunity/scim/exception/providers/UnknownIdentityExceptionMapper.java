/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.exception.providers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.identity.UnknownIdentityException;

@Provider
class UnknownIdentityExceptionMapper implements ExceptionMapper<UnknownIdentityException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UnknownIdentityExceptionMapper.class);

	public Response toResponse(UnknownIdentityException ex)
	{

		log.debug("Unknown identity", ex);
		return Response.status(Status.NOT_FOUND).entity(ErrorResponse.builder()
				.withStatus(Status.NOT_FOUND.getStatusCode()).withDetail("Invalid user").build().toJsonString())
				.type(MediaType.APPLICATION_JSON).build();

	}
}
