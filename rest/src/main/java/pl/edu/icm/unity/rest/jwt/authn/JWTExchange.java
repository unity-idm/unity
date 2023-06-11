/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.jwt.authn;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;


/**
 * Exchange of JWT credential
 * @author K. Benedyczak
 */
public interface JWTExchange extends CredentialExchange
{
	public static final String ID = "JWT exchange";
	
	public AuthenticationResult checkJWT(String token) throws EngineException, AuthenticationException;
}
