/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as.token;

import java.util.Date;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.ParseException;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;
import com.nimbusds.oauth2.sdk.token.BearerTokenError;

import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;

/**
 * Common code inherited by OAuth resources
 */
public class BaseTokenResource extends BaseOAuthResource
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, BaseTokenResource.class);
	
	private final OAuthAccessTokenRepository tokensDAO;
	
	public BaseTokenResource(OAuthAccessTokenRepository tokensDAO)
	{
		this.tokensDAO = tokensDAO;
	}

	protected TokensPair resolveBearerToken(String bearerToken) throws OAuthTokenException
	{
		if (bearerToken == null)
			throw new OAuthTokenException(
					makeBearerError(BearerTokenError.MISSING_TOKEN, "To access this endpoint "
					+ "an access token must be used for authorization"));
		
		BearerAccessToken accessToken;
		try
		{
			accessToken = BearerAccessToken.parse(bearerToken);
		} catch (ParseException e)
		{
			throw new OAuthTokenException(
					makeBearerError(BearerTokenError.INVALID_TOKEN, "Must use Bearer access token"));
		}
		
		try
		{
			Token rawToken = tokensDAO.readAccessToken(accessToken.getValue()); 
			OAuthToken parsedAccessToken = parseInternalToken(rawToken);
			return new TokensPair(rawToken, parsedAccessToken);
		} catch (IllegalArgumentException e)
		{
			throw new OAuthTokenException(makeBearerError(BearerTokenError.INVALID_TOKEN));
		}
	}

	protected void extendValidityIfNeeded(Token rawToken, OAuthToken parsedAccessToken) throws OAuthTokenException
	{
		long newExpiry = System.currentTimeMillis() + ((long)parsedAccessToken.getTokenValidity()*1000);
		long maxExpiry = rawToken.getCreated().getTime() + ((long)parsedAccessToken.getMaxExtendedValidity()*1000);
		if (maxExpiry < newExpiry)
			newExpiry = maxExpiry;
		if (newExpiry > rawToken.getExpires().getTime())
		{
			try
			{
				Date newExpiryDate = new Date(newExpiry);
				log.debug("Extending token {} expiration from {} to {}",
						"..." + parsedAccessToken.getAccessToken().substring(6),
						new Date(rawToken.getExpires().getTime()), newExpiryDate);
				tokensDAO.updateAccessTokenExpiration(rawToken, newExpiryDate);
			} catch (IllegalArgumentException e)
			{
				log.warn("Can't update access token validity, this shouldn't happen", e);
				throw new OAuthTokenException(makeBearerError(BearerTokenError.INVALID_TOKEN));
			}
			rawToken.setExpires(new Date(newExpiry));
		}
	}
	
	
	public static class TokensPair
	{
		public final Token tokenSrc;
		public final OAuthToken parsedToken;

		public TokensPair(Token tokenSrc, OAuthToken parsedToken)
		{
			super();
			this.tokenSrc = tokenSrc;
			this.parsedToken = parsedToken;
		}
	}
}
