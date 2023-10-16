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

import com.fasterxml.jackson.core.JsonParseException;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.base.utils.Log;

@Provider
class JSONParseExceptionMapper implements ExceptionMapper<JsonParseException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, JSONParseExceptionMapper.class);

	public Response toResponse(JsonParseException ex)
	{
		log.error("JSON parse error during RESTful API invocation", ex);
		return Response
				.status(Status.BAD_REQUEST).entity(OAuthExceptionMapper
						.makeError(OAuth2Error.SERVER_ERROR, "JSON parse error").toJSONObject().toJSONString())
				.type(MediaType.APPLICATION_JSON).build();
	}
}
