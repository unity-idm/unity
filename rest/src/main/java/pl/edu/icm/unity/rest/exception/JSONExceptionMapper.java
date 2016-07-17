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

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.types.JsonError;

/**
 * Maps JSON exceptions to HTTP error responses
 * @author K. Benedyczak
 */
@Provider
public class JSONExceptionMapper implements ExceptionMapper<JsonProcessingException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, JSONExceptionMapper.class);
	
	public Response toResponse(JsonProcessingException ex)
	{
		log.error("JSON error during RESTful API invocation", ex);
		return Response.status(Status.INTERNAL_SERVER_ERROR).entity(new JsonError(ex).toString()).
				type(MediaType.APPLICATION_JSON).build();
	}
}
