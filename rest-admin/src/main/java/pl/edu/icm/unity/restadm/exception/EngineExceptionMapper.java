/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.restadm.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.exceptions.AuthorizationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.utils.Log;

/**
 * Maps Unity exceptions to HTTP error responses
 * @author K. Benedyczak
 */
@Provider
public class EngineExceptionMapper implements ExceptionMapper<EngineException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, EngineExceptionMapper.class);
	
	public Response toResponse(EngineException ex)
	{
		if (ex instanceof AuthorizationException)
		{
			log.debug("Access denied for rest client", ex);
			return Response.status(Status.FORBIDDEN).entity(ex.getMessage()).build();
		} else
		{
			log.warn("Engine exception during RESTful API invocation", ex);
			return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
		}
	}
}
