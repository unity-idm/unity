/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.scim.exceptions.mapper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import io.imunity.scim.exceptions.SCIMException;

public class SCIMExceptionMapper implements ExceptionMapper<SCIMException>
{

	@Override
	public Response toResponse(SCIMException exception)
	{
		return Response.status(exception.scimError.status).entity(exception.scimError.toJsonString()).
				type(MediaType.APPLICATION_JSON).build();
	}

}
