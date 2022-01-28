/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.scim.exceptions.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import io.imunity.scim.messages.ErrorResponse;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.exceptions.AuthorizationException;

@Provider
public class EngineExceptionMapper implements ExceptionMapper<Exception>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, EngineExceptionMapper.class);

	public Response toResponse(Exception ex)
	{
		if (ex instanceof AuthorizationException || ex instanceof AuthenticationException)
		{
			log.warn("Access denied for SCIM API client", ex);
			return Response.status(Status.FORBIDDEN).entity(ErrorResponse.builder()
					.withStatus(Status.FORBIDDEN.getStatusCode()).withDetail("Forbidden").build().toJsonString())
					.type(MediaType.APPLICATION_JSON).build();
		} else
		{
			log.warn("Engine exception during SCIM API invocation", ex);
			return Response.status(Status.BAD_REQUEST).entity(ErrorResponse.builder()
					.withStatus(Status.BAD_REQUEST.getStatusCode()).withDetail("Bad Request").build().toJsonString())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
}
