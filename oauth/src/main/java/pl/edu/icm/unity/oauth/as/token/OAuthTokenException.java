/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
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
