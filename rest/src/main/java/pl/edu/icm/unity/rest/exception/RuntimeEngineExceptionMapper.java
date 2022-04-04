/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.rest.exception;

import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.RuntimeEngineException;
import pl.edu.icm.unity.types.JsonError;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class RuntimeEngineExceptionMapper implements ExceptionMapper<RuntimeEngineException>
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_REST, RuntimeEngineExceptionMapper.class);
	private final EngineExceptionMapper engineExceptionMapper = new EngineExceptionMapper();

	public Response toResponse(RuntimeEngineException ex)
	{
		if(ex.getCause() instanceof EngineException)
			engineExceptionMapper.toResponse((EngineException) ex.getCause());

		log.warn("Engine runtime exception during RESTful API invocation", ex);
		return Response.status(Response.Status.BAD_REQUEST).entity(new JsonError(ex).toString()).
				type(MediaType.APPLICATION_JSON).build();
	}
}
