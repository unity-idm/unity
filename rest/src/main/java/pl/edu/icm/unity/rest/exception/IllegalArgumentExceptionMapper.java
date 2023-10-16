/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;

/**
 * Maps {@link IllegalArgumentException} to HTTP error response
 * @author K. Benedyczak
 */
@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, IllegalArgumentExceptionMapper.class);
	
	public Response toResponse(IllegalArgumentException ex)
	{
		log.warn("IllegalArgumentException exception during RESTful API invocation", ex);
		return Response.status(Status.BAD_REQUEST).entity(new JsonError(ex).toString()).
					type(MediaType.APPLICATION_JSON).build();
	}
}
