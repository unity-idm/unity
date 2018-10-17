/*
 * Copyright (c) 2016 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import javax.ws.rs.core.Response;

/**
 * Thrown when an error is found and error response should be returned by the root 
 * REST method.
 * @author Krzysztof Benedyczak
 */
public class OAuthTokenException extends Exception
{
	private Response errorResponse;

	public OAuthTokenException(Response response)
	{
		super();
		this.errorResponse = response;
	}

	public Response getErrorResponse()
	{
		return errorResponse;
	}
}
