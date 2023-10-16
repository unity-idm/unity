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
 * Maps NullPointerException to HTTP error responses. No details are exposed to the client.
 * @author K. Benedyczak
 */
@Provider
public class NPEExceptionMapper implements ExceptionMapper<NullPointerException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, NPEExceptionMapper.class);
	
	public Response toResponse(NullPointerException ex)
	{
		log.error("NullPointerException error during RESTful API invocation", ex);
		return Response.status(Status.INTERNAL_SERVER_ERROR).
				type(MediaType.APPLICATION_JSON).build();
	}
}
