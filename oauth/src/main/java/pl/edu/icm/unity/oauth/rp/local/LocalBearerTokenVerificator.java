/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.local;

import java.util.List;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.oauth.as.OAuthTokenRepository;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties;
import pl.edu.icm.unity.oauth.rp.verificator.InternalTokenVerificator;
import pl.edu.icm.unity.oauth.rp.verificator.TokenStatus;

class LocalBearerTokenVerificator
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, LocalBearerTokenVerificator.class);

	private final InternalTokenVerificator tokenChecker;
	private final LocalOAuthRPProperties verificatorProperties;

	public LocalBearerTokenVerificator(OAuthTokenRepository tokensDAO, LocalOAuthRPProperties verificatorProperties)
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
			throw new AuthenticationException("Authentication error ocurred", e);
		}
	}

	AuthenticationResultWithTokenStatus checkTokenInterruptible(BearerAccessToken token) throws Exception
	{
		TokenStatus status = tokenChecker.checkToken(token);
		if (status.isValid())
		{
			if (!checkScopes(status))
			{
				return new AuthenticationResultWithTokenStatus(LocalAuthenticationResult.failed(), status);
			}

			AuthenticatedEntity ae = new AuthenticatedEntity(status.getOwnerId().get(), status.getSubject(), null);
			return new AuthenticationResultWithTokenStatus(LocalAuthenticationResult.successful(ae), status);

		} else
		{
			return new AuthenticationResultWithTokenStatus(LocalAuthenticationResult.failed(), status);
		}
	}

	private boolean checkScopes(TokenStatus status)
	{
		List<String> required = verificatorProperties.getListOfValues(OAuthRPProperties.REQUIRED_SCOPES);
		if (!required.isEmpty() && status.getScope() == null)
		{
			log.debug("The token validation didn't provide any scope, but there are required scopes");
			return false;
		}
		required.removeAll(status.getScope().toStringList());
		if (!required.isEmpty())
		{
			log.debug("The following required scopes are not present: " + required);
			return false;
		}
		return true;
	}
}
