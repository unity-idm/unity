/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.google.common.collect.Sets;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.verificator.InternalTokenVerificator;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;

class LocalBearerTokenVerificator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, LocalBearerTokenVerificator.class);

	private final InternalTokenVerificator tokenChecker;
	private final LocalOAuthRPProperties verificatorProperties;

	public LocalBearerTokenVerificator(OAuthAccessTokenRepository tokensDAO, LocalOAuthRPProperties verificatorProperties)
	{
		this.tokenChecker = new InternalTokenVerificator(tokensDAO);
		this.verificatorProperties = verificatorProperties;
	}

	AuthenticationResultWithTokenStatus checkToken(BearerAccessToken token) throws AuthenticationException
	{
		try
		{
			return checkTokenInterruptible(token);
		} catch (AuthenticationException e)
		{
			throw e;
		} catch (Exception e)
		{
			throw new AuthenticationException("Authentication error occurred", e);
		}
	}

	AuthenticationResultWithTokenStatus checkTokenInterruptible(BearerAccessToken token) throws Exception
	{
		TokenStatus status = tokenChecker.checkToken(token);
		if (status.isValid())
		{
			if (!areMandatoryScopesPresent(status))
			{
				log.debug("Bearer access token " + token.getValue() + " has no mandatory scopes");
				return new AuthenticationResultWithTokenStatus(LocalAuthenticationResult.failed(), status);
			}

			AuthenticatedEntity ae = new AuthenticatedEntity(status.getOwnerId().get(), status.getSubject(), null);
			return new AuthenticationResultWithTokenStatus(LocalAuthenticationResult.successful(ae, AuthenticationMethod.UNKNOWN), status);

		} else
		{
			return new AuthenticationResultWithTokenStatus(LocalAuthenticationResult.failed(), status);
		}
	}

	private boolean areMandatoryScopesPresent(TokenStatus status)
	{
		Set<String> requiredScopes = Set.copyOf(verificatorProperties.getListOfValues(OAuthRPProperties.REQUIRED_SCOPES));
		if (!requiredScopes.isEmpty() && status.getScope() == null)
		{
			log.debug("The token validation didn't provide any scope, but there are required scopes");
			return false;
		}
		Set<String> requestedScopes = Set.copyOf(status.getScope().toStringList());
		if (!requestedScopes.containsAll(requiredScopes))
		{
			log.debug("The following required scopes are not present in token: {}",
					Sets.difference(requiredScopes, requestedScopes));
			return false;
		}
		return true;
	}
}
