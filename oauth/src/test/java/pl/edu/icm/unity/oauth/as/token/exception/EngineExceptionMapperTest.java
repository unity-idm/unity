/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class EngineExceptionMapperTest
{
	@Test
	public void shouldPreserveClientErrorResponse()
	{
		Response originalResponse = Response.status(Response.Status.NOT_ACCEPTABLE).build();

		Response response = new EngineExceptionMapper().toResponse(new ClientErrorException(originalResponse));

		assertThat(response.getStatus()).isEqualTo(Response.Status.NOT_ACCEPTABLE.getStatusCode());
	}
}
