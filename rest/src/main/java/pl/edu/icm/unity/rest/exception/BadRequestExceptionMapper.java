/*
 * Copyright (c) 2023 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, BadRequestExceptionMapper.class);
	
	@Override
	public Response toResponse(BadRequestException ex)
	{
		log.warn("BadRequestException exception during RESTful API invocation", ex);
		return Response.status(Status.BAD_REQUEST).entity(new JsonError(ex).toString()).
					type(MediaType.APPLICATION_JSON).build();
	}
}
