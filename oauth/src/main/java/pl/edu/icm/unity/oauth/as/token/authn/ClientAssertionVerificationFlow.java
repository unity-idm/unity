/*
 * Copyright (c) 2024 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token.authn;

import java.net.URI;

import org.apache.logging.log4j.Logger;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;

class ClientAssertionVerificationFlow
{
	record JwksResolution(long entityId, JWKSet jwks) {}

	@FunctionalInterface
	interface JwksResolver
	{
		JwksResolution resolve(String clientId) throws Exception;
	}

	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, ClientAssertionVerificationFlow.class);

	private final JwtClientAssertionVerifier jwtVerifier = new JwtClientAssertionVerifier();

	AuthenticationResult verify(String assertion, URI canonicalTokenEndpointUri,
			ResolvableError error, JwksResolver resolver)
	{
		SignedJWT jwt;
		try
		{
			jwt = SignedJWT.parse(assertion);
		} catch (Exception e)
		{
			log.debug("Failed to parse client_assertion JWT", e);
			return LocalAuthenticationResult.failed(error);
		}

		String clientId;
		try
		{
			clientId = jwt.getJWTClaimsSet().getSubject();
			if (clientId == null)
			{
				log.debug("client_assertion JWT has no sub claim");
				return LocalAuthenticationResult.failed(error);
			}
		} catch (Exception e)
		{
			log.debug("Failed to read claims from client_assertion JWT", e);
			return LocalAuthenticationResult.failed(error);
		}

		JwksResolution resolution;
		try
		{
			resolution = resolver.resolve(clientId);
		} catch (Exception e)
		{
			return LocalAuthenticationResult.failed(error);
		}

		try
		{
			jwtVerifier.verifyJwt(jwt, resolution.jwks(), canonicalTokenEndpointUri, clientId);
		} catch (AuthenticationException e)
		{
			log.info("JWT assertion verification failed for client {}: {}", clientId, e.getMessage());
			return LocalAuthenticationResult.failed(error);
		}

		AuthenticatedEntity ae = new AuthenticatedEntity(resolution.entityId(), clientId, null);
		return LocalAuthenticationResult.successful(ae, AuthenticationMethod.UNKNOWN);
	}
}
