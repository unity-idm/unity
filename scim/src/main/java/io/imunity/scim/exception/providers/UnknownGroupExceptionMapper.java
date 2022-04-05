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
import pl.edu.icm.unity.exceptions.IllegalGroupValueException;

@Provider
class UnknownGroupExceptionMapper implements ExceptionMapper<IllegalGroupValueException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, UnknownGroupExceptionMapper.class);

	public Response toResponse(IllegalGroupValueException ex)
	{

		log.debug("Unknown group", ex);
		return Response.status(Status.NOT_FOUND).entity(ErrorResponse.builder()
				.withStatus(Status.NOT_FOUND.getStatusCode()).withDetail("Invalid group").build().toJsonString())
				.type(MediaType.APPLICATION_JSON).build();

	}
}
