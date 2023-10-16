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

import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;

@Provider
class EngineExceptionMapper implements ExceptionMapper<Exception>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, EngineExceptionMapper.class);

	public Response toResponse(Exception ex)
	{
		if (ex instanceof AuthorizationException || ex instanceof AuthenticationException)
		{
			log.warn("Access denied for rest client", ex);
			return Response.status(Status.FORBIDDEN)
					.entity(OAuthExceptionMapper
							.makeError(OAuth2Error.INVALID_CLIENT, "Authentication failed")
							.toJSONObject().toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		} else
		{
			log.warn("Engine exception during RESTful API invocation", ex);
			return Response
					.status(Status.BAD_REQUEST).entity(OAuthExceptionMapper
							.makeError(OAuth2Error.SERVER_ERROR, "Server engine error")
							.toJSONObject().toJSONString())
					.type(MediaType.APPLICATION_JSON).build();
		}
	}
}
