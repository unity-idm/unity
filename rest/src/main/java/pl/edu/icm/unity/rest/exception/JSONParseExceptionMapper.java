/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.exception;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.server.utils.Log;

import com.fasterxml.jackson.core.JsonParseException;

/**
 * Maps JSON parse exceptions to HTTP error responses
 * @author K. Benedyczak
 */
@Provider
public class JSONParseExceptionMapper implements ExceptionMapper<JsonParseException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, JSONParseExceptionMapper.class);
	
	public Response toResponse(JsonParseException ex)
	{
		log.error("JSON parse error during RESTful API invocation", ex);
		return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
	}
}
