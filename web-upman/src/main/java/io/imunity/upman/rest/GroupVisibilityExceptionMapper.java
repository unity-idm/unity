package io.imunity.upman.rest;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import pl.edu.icm.unity.engine.api.group.GroupVisibilityException;
import pl.edu.icm.unity.rest.exception.JsonError;

class GroupVisibilityExceptionMapper implements ExceptionMapper<GroupVisibilityException>
{
	@Override
	public Response toResponse(GroupVisibilityException exception)
	{
		return Response.status(Response.Status.CONFLICT)
				.entity(new JsonError(exception).toString())
				.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
