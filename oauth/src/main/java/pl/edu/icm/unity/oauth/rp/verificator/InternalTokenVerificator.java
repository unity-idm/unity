/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.rp.verificator;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.oauth.as.OAuthProcessor;
import pl.edu.icm.unity.oauth.as.OAuthToken;
import pl.edu.icm.unity.server.api.internal.Token;
import pl.edu.icm.unity.server.api.internal.TokensManagement;

import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.token.BearerAccessToken;

/**
 * Verifies the token against internal Unity's token storage, i.e. the token is checked if was 
 * issued by the local system. 
 * 
 * @author K. Benedyczak
 */
public class InternalTokenVerificator implements TokenVerificatorProtocol
{
	private TokensManagement tokensManagement;
	
	public InternalTokenVerificator(TokensManagement tokensMan)
	{
		this.tokensManagement = tokensMan;
	}


	@Override
	public TokenStatus checkToken(BearerAccessToken token) throws Exception
	{
		Token internalAccessToken;
		try
		{
			internalAccessToken = tokensManagement.getTokenById(OAuthProcessor.INTERNAL_ACCESS_TOKEN, 
					token.getValue());
		} catch (WrongArgumentException e)
		{
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
		
		Scope scope = new Scope(internalToken.getScope());
		return new TokenStatus(true, internalAccessToken.getExpires(), scope, internalToken.getSubject());
	}

}
