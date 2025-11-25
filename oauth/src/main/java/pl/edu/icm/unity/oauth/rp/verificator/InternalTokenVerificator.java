/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import org.apache.logging.log4j.Logger;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

import pl.edu.icm.unity.base.exceptions.InternalException;
import pl.edu.icm.unity.base.token.Token;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.oauth.as.token.access.OAuthAccessTokenRepository;

/**
 * Verifies the token against internal Unity's token storage, i.e. the token is checked if was 
 * issued by the local system. 
 * 
 * @author K. Benedyczak
 */
public class InternalTokenVerificator implements TokenVerificatorProtocol
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, InternalTokenVerificator.class);
	
	private OAuthAccessTokenRepository tokensDAO;
	
	public InternalTokenVerificator(OAuthAccessTokenRepository tokensDAO)
	{
		this.tokensDAO = tokensDAO;
	}


	@Override
	public TokenStatus checkToken(BearerAccessToken token) throws Exception
	{
		Token internalAccessToken;
		try
		{
			internalAccessToken = tokensDAO.readAccessToken(token.getValue());
		} catch (IllegalArgumentException e)
		{
			log.debug("Invalid bearer access token");
			return new TokenStatus();
		}
		
		OAuthToken internalToken;
		try
		{
			internalToken = OAuthToken.getInstanceFromJson(internalAccessToken.getContents());
		} catch (Exception e)
		{
			throw new InternalException("Can not parse the internal code token", e);
		}
		
		Scope scope = new Scope(internalToken.getEffectiveScopeAsString());
		return new TokenStatus(true, internalAccessToken.getExpires(), scope, internalToken.getSubject(), internalAccessToken.getOwner(), internalToken.getClientId());
	}

}
