/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.as.token.introspection;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.TokenIntrospectionRequest;
import com.nimbusds.oauth2.sdk.TokenIntrospectionResponse;
import com.nimbusds.oauth2.sdk.auth.ClientSecretBasic;
import com.nimbusds.oauth2.sdk.auth.Secret;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.http.HTTPResponse;
import com.nimbusds.oauth2.sdk.id.ClientID;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.oauth.as.token.BaseOAuthResource;
import pl.edu.icm.unity.oauth.client.HttpRequestConfigurer;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthDiscoveryMetadataCache;
import pl.edu.icm.unity.oauth.oidc.metadata.OAuthJWKSetCache;

class RemoteTokenIntrospectionService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, RemoteTokenIntrospectionService.class);
	private IntrospectionServiceContextProvider introspectionServiceProvider;
	private HttpRequestConfigurer httpRequestConfigurer;

	RemoteTokenIntrospectionService(IntrospectionServiceContextProvider introspectionServiceProvider,
			HttpRequestConfigurer httpRequestConfigurer)
	{
		this.introspectionServiceProvider = introspectionServiceProvider;
		this.httpRequestConfigurer = httpRequestConfigurer;
	}

	RemoteTokenIntrospectionService(IntrospectionServiceContextProvider introspectionServiceProvider)
	{
		this(introspectionServiceProvider, new HttpRequestConfigurer());

	}

	public Response processRemoteIntrospection(SignedJWTWithIssuer signedJWTWithIssuer)
	{
		log.debug("Remote token introspection, token {}",
				BaseOAuthResource.tokenToLog(signedJWTWithIssuer.signedJWT.toString()));
		Optional<TokenIntrospectionResponse> remoteResponse = proxyRequestToRemoteService(signedJWTWithIssuer);
		if (remoteResponse.isEmpty())
		{
			return Response.ok(TokenIntrospectionResource.getInactiveResponse()
					.toJSONString())
					.build();
		} else
		{
			log.debug("Remote token instrospection response {}", remoteResponse.get().toHTTPResponse().getContent());

			return Response.ok(remoteResponse.get()
					.indicatesSuccess()
							? remoteResponse.get()
									.toSuccessResponse()
									.toJSONObject()
									.toJSONString()
							: TokenIntrospectionResource.getInactiveResponse()
									.toJSONString())
					.build();
		}
	}

	private Optional<TokenIntrospectionResponse> proxyRequestToRemoteService(SignedJWTWithIssuer signedJWT)
	{
		Optional<RemoteIntrospectionServiceContext> remoteService = introspectionServiceProvider
				.getRemoteServiceContext(signedJWT.issuer);
		if (remoteService.isEmpty())
		{
			log.debug("Remote introspection configuration is unknown for token issued by {}", signedJWT.issuer);
			return Optional.empty();
		}

		RemoteIntrospectionServiceContext serviceContext = remoteService.get();

		try
		{
			verifySignature(signedJWT.signedJWT, serviceContext.verifier);
		} catch (Exception e)
		{
			log.error("Invalid sign of token " + BaseOAuthResource.tokenToLog(signedJWT.signedJWT.toString()), e);
			return Optional.empty();
		}

		return getRemoteIntrospectionResponse(serviceContext, signedJWT);

	}

	private Optional<TokenIntrospectionResponse> getRemoteIntrospectionResponse(
			RemoteIntrospectionServiceContext service, SignedJWTWithIssuer signedJWTWithIssuer)
	{
		TokenIntrospectionRequest request;
		try
		{
			request = new TokenIntrospectionRequest(service.url.toURI(),
					new ClientSecretBasic(new ClientID(service.clientId), new Secret(service.clientSecret)),
					new BearerAccessToken(signedJWTWithIssuer.signedJWT.serialize()));
		} catch (Exception e)
		{
			log.error("Invalid remote introspection service configuration", e);
			return Optional.empty();
		}
		HTTPRequest secureRequest = httpRequestConfigurer.secureRequest(request.toHTTPRequest(), service.validator,
				service.hostnameCheckingMode);
		try
		{
			log.debug("Get token instrospection response from {}", service.url);
			HTTPResponse response = secureRequest.send();
			return Optional.of(TokenIntrospectionResponse.parse(response));

		} catch (IOException e)
		{
			log.error("Can not send introspection request", e);
			return Optional.empty();
		} catch (ParseException e)
		{
			log.error("Can not parse token instrospection response", e);
			return Optional.empty();
		}
	}

	void verifySignature(SignedJWT signedJWT, JWSVerifier verifier) throws JOSEException, java.text.ParseException
	{
		if (verifier == null)
		{
			throw new JOSEException("Can not verify signature");
		}
		log.trace("Verify token sign");
		if (!signedJWT.verify(verifier))
		{
			throw new JOSEException("JWT signature is invalid");
		}

		JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
		if (new Date().after(claims.getExpirationTime()))
		{
			throw new JOSEException("JWT is expired");
		}
	}

	@Component
	public static class RemoteIntrospectionServiceFactory
	{
		private OAuthDiscoveryMetadataCache oAuthDiscoveryMetadataCache;
		private OAuthJWKSetCache keyResourceCache;
		private PKIManagement pkiManagement;

		@Autowired
		public RemoteIntrospectionServiceFactory(OAuthDiscoveryMetadataCache oAuthDiscoveryMetadataCache,
				OAuthJWKSetCache keyResourceCache, @Qualifier("insecure") PKIManagement pkiManagement)
		{

			this.oAuthDiscoveryMetadataCache = oAuthDiscoveryMetadataCache;
			this.keyResourceCache = keyResourceCache;
			this.pkiManagement = pkiManagement;
		}

		RemoteTokenIntrospectionService getService(List<TrustedUpstreamConfiguration> config)
		{
			return new RemoteTokenIntrospectionService(new IntrospectionServiceContextProvider(
					oAuthDiscoveryMetadataCache, keyResourceCache, pkiManagement, config));

		}
	}
}
