/*
 * Copyright (c) 2024 Bixbit s.c. All rights reserved.
 *  See LICENSE file for licensing information.
 */
package io.imunity.jwt;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import java.net.URI;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.springframework.stereotype.Component;

@Produces(APPLICATION_JSON)
@Path(AuthzLoginTokenController.AUTHZ_LOGIN_TOKEN_PATH)
public class AuthzLoginTokenController
{
	public static final String AUTHZ_LOGIN_TOKEN_PATH = "/authzLoginToken";
	
	private final AuthzLoginTokenService service;

	AuthzLoginTokenController(AuthzLoginTokenService service)
	{
		this.service = service;
	}

	@Path("/")
	@POST
	public AuthzLoginTokenResponse getAuthzLoginToken(AuthzLoginTokenRequest request)
	{
		return new AuthzLoginTokenResponse(service.getAuthzLoginToken(request.jwtHash, request.redirectURL));
	}
	
	public static class AuthzLoginTokenResponse
	{
		public final String token;

		public AuthzLoginTokenResponse(String token)
		{
			this.token = token;
		}
	}
	
	public static class AuthzLoginTokenRequest
	{
		public final String jwtHash;
		public final URI redirectURL;
		
		public AuthzLoginTokenRequest(String jwtHash, URI redirectURL)
		{
			this.jwtHash = jwtHash;
			this.redirectURL = redirectURL;
		}
	}

	@Component
	static class JWTAuthenticationControllerFactory
	{
		private final AuthzLoginTokenService service;
		
		JWTAuthenticationControllerFactory(AuthzLoginTokenService service)
		{
			this.service = service;
		}

		AuthzLoginTokenController get(String configuration)
		{
			return new AuthzLoginTokenController(service);
		}
	}
}
