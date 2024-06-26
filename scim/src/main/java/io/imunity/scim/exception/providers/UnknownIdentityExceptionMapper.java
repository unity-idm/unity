/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.exception.providers;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.UnknownIdentityException;

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
