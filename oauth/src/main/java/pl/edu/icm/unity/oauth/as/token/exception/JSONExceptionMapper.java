/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.base.utils.Log;

@Provider
class JSONExceptionMapper implements ExceptionMapper<JsonProcessingException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, JSONExceptionMapper.class);

	public Response toResponse(JsonProcessingException ex)
	{
		log.error("JSON error during RESTful API invocation", ex);
		return Response
				.status(Status.BAD_REQUEST).entity(OAuthExceptionMapper
						.makeError(OAuth2Error.SERVER_ERROR, "JSON processing error").toJSONObject().toJSONString())
				.type(MediaType.APPLICATION_JSON).build();
	}
}
