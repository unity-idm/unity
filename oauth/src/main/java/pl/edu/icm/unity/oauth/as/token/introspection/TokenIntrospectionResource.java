/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.introspection;

import java.text.ParseException;
import java.util.Optional;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.OAuth2Error;

import net.minidev.json.JSONObject;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.as.OAuthASProperties;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.as.token.OAuthErrorException;
import pl.edu.icm.unity.oauth.as.token.OAuthTokenEndpoint;
import pl.edu.icm.unity.oauth.as.token.TokenInfoResource;
import pl.edu.icm.unity.oauth.as.token.introspection.LocalTokenIntrospectionService.LocalTokenIntrospectionServiceFactory;
import pl.edu.icm.unity.oauth.as.token.introspection.RemoteTokenIntrospectionService.RemoteIntrospectionServiceFactory;

/**
 * Implementation of RFC 7662 - OAuth 2.0 Token Introspection. Similar to
 * (older) {@link TokenInfoResource}, however standard, with more restricted
 * authorization. Currently only supports bearer tokens (refresh and access).
 * May be enhanced to support signed openId tokens but that makes little sense
 * (those are intended for self validation).
 */
@Produces("application/json")
@Path(OAuthTokenEndpoint.TOKEN_INTROSPECTION_PATH)
public class TokenIntrospectionResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, TokenIntrospectionResource.class);

	private final RemoteTokenIntrospectionService remoteTokenIntrospectionService;
	private final LocalTokenIntrospectionService localTokenIntrospectionService;
	private final String localIssuer;

	public TokenIntrospectionResource(RemoteTokenIntrospectionService remoteTokenIntrospectionService,
			LocalTokenIntrospectionService localTokenIntrospectionService, String localIssuer)
	{

		this.remoteTokenIntrospectionService = remoteTokenIntrospectionService;
		this.localTokenIntrospectionService = localTokenIntrospectionService;
		this.localIssuer = localIssuer;
	}

	@Path("/")
	@POST
	public Response introspectToken(@FormParam("token") String token) throws EngineException, JsonProcessingException
	{
		if (token == null)
			throw new OAuthErrorException(
					makeError(OAuth2Error.INVALID_REQUEST, "Token for introspection was not provided"));

		log.debug("Token introspection enquiry for token {}", tokenToLog(token));

		Optional<SignedJWTWithIssuer> signedJWT = tryParseAsSignedJWTToken(token);
		boolean proxyToRemoteService = signedJWT.isPresent() && !signedJWT.get().issuer.equals(localIssuer);
		return proxyToRemoteService ? remoteTokenIntrospectionService.processRemoteIntrospection(signedJWT.get())
				: localTokenIntrospectionService.processLocalIntrospection(token);
	}

	private Optional<SignedJWTWithIssuer> tryParseAsSignedJWTToken(String token)
	{
		SignedJWT signedJWT = null;
		try
		{
			signedJWT = SignedJWT.parse(token);
		} catch (ParseException e)
		{
			return Optional.empty();
		}

		try
		{
			return Optional.of(new SignedJWTWithIssuer(signedJWT));
		} catch (ParseException e)
		{
			log.trace("Unknown issuer of token {}", tokenToLog(signedJWT.serialize()));
			return Optional.empty();
		}
	}

	public static JSONObject getInactiveResponse()
	{
		JSONObject ret = new JSONObject();
		ret.put("active", false);
		return ret;
	}

	@Component
	public static class TokenIntrospectionResourceFactory
	{
		private final RemoteIntrospectionServiceFactory remoteIntrospectionServiceFactory;
		private final LocalTokenIntrospectionServiceFactory localIntrospectionServiceFactory;

		@Autowired
		public TokenIntrospectionResourceFactory(LocalTokenIntrospectionServiceFactory localIntrospectionServiceFactory,
				RemoteIntrospectionServiceFactory remoteIntrospectionServiceFactory)
		{
			this.localIntrospectionServiceFactory = localIntrospectionServiceFactory;
			this.remoteIntrospectionServiceFactory = remoteIntrospectionServiceFactory;
		}

		public TokenIntrospectionResource getTokenIntrospection(OAuthASProperties config)
		{
			return new TokenIntrospectionResource(
					remoteIntrospectionServiceFactory.getService(TrustedUpstreamConfigurationParser.getConfig(config)),
					localIntrospectionServiceFactory.getService(), config.getValue(OAuthASProperties.ISSUER_URI));
		}
	}

}
