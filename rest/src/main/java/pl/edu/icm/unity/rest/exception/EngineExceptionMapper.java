/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.types.JsonError;

/**
 * Maps Unity exceptions to HTTP error responses
 * @author K. Benedyczak
 */
@Provider
public class EngineExceptionMapper implements ExceptionMapper<Exception>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, EngineExceptionMapper.class);
	
	public Response toResponse(Exception ex)
	{
		if (ex instanceof AuthorizationException || ex instanceof AuthenticationException)
		{
			log.warn("Access denied for rest client", ex);
			return Response.status(Status.FORBIDDEN).entity(new JsonError(ex).toString()).
					type(MediaType.APPLICATION_JSON).build();
		} else
		{
			log.warn("Engine exception during RESTful API invocation", ex);
			return Response.status(Status.BAD_REQUEST).entity(new JsonError(ex).toString()).
					type(MediaType.APPLICATION_JSON).build();
		}
	}
}
