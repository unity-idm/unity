/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exception.providers;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.apache.logging.log4j.Logger;

import io.imunity.scim.exception.SCIMException;
import pl.edu.icm.unity.base.utils.Log;

@Provider
class SCIMExceptionMapper implements ExceptionMapper<SCIMException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SCIM, SCIMExceptionMapper.class);

	@Override
	public Response toResponse(SCIMException exception)
	{
		log.debug("SCIM exception during SCIM API invocation", exception);
		return Response.status(exception.statusCode)
				.entity(ErrorResponse.builder().withStatus(exception.statusCode).withDetail(exception.errorMessage)
						.withScimType(exception.scimType).build().toJsonString())
				.type(MediaType.APPLICATION_JSON).build();

	}

}
