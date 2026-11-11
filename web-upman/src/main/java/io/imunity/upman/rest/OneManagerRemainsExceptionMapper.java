package io.imunity.upman.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import pl.edu.icm.unity.engine.api.project.OneManagerRemainsException;
import pl.edu.icm.unity.rest.exception.JsonError;

class OneManagerRemainsExceptionMapper implements ExceptionMapper<OneManagerRemainsException>
{
	@Override
	public Response toResponse(OneManagerRemainsException exception)
	{
		return Response.status(Response.Status.CONFLICT)
				.entity(new JsonError(exception).toString())
				.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
