/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;

/**
 * Utility methods processing results of authenticators.
 * 
 * @author K. Benedyczak
 */
public interface AuthenticationProcessor
{
	/**
	 * @return authenticator which is valid for the given entity from the given pool
	 */
	AuthenticatorInstance getValidAuthenticatorForEntity(Collection<AuthenticatorInstance> pool, long entityId);
	
	/**
	 * @return true only if user can use the given authenticator. Works well (currently) only for local authenticators
	 */
	boolean checkIfUserHasCredential(AuthenticatorInstanceMetadata authn, long entityId);
	
	/**
	 * Starting point: the result of the primary authenticator is verified. If the authentication failed
	 * then an exception is thrown. Otherwise it is checked whether, according to the 
	 * {@link AuthenticationFlow} selected, second authentication should be performed, what is returned.
	 */
	PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result, 
			AuthenticationFlow authenticationFlow, AuthenticationOptionKey authnOptionId) throws AuthenticationException;
	
	
	/**
	 * Should be used if the second step authentication is not required: retrieve a final {@link AuthenticatedEntity}.
	 */
	AuthenticatedEntity finalizeAfterPrimaryAuthentication(PartialAuthnState state, boolean skipSecondFactor);
	

	
	/**
	 * Should be used if the second step authentication is required to process second authenticator results
	 * and retrieve a final {@link AuthenticatedEntity}.
	 * @param primaryResult
	 * @return
	 * @throws AuthenticationException 
	 */
	AuthenticatedEntity finalizeAfterSecondaryAuthentication(PartialAuthnState state, 
			AuthenticationResult result2) throws AuthenticationException;
	
	/**
	 * Extracts and returns all remote {@link SessionParticipant}s from the {@link AuthenticationResult}s.
	 */
	public static List<SessionParticipant> extractParticipants(AuthenticationResult... results) 
	{
		List<SessionParticipant> ret = new ArrayList<>();
		for (AuthenticationResult result: results)
		{
			if (result.isRemote() && result.asRemote().getSuccessResult().remotePrincipal.getSessionParticipants() != null)
				ret.addAll(result.asRemote().getSuccessResult().remotePrincipal.getSessionParticipants());
		}
		return ret;
	}
}
