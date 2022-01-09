/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.OAuth2Error;

import pl.edu.icm.unity.base.utils.Log;

@Provider
class NPEExceptionMapper implements ExceptionMapper<NullPointerException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, NPEExceptionMapper.class);

	public Response toResponse(NullPointerException ex)
	{
		log.error("NullPointerException error during RESTful API invocation", ex);
		return Response
				.status(Status.BAD_REQUEST).entity(OAuthExceptionMapper
						.makeError(OAuth2Error.SERVER_ERROR, ex.getMessage()).toJSONObject().toJSONString())
				.type(MediaType.APPLICATION_JSON).build();
	}
}