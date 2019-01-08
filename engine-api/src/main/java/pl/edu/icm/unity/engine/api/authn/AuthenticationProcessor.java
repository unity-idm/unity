/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;

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
			AuthenticationFlow authenticationFlow, String authnOptionId) throws AuthenticationException;
	
	
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
	 * @param results
	 * @return
	 * @throws AuthenticationException
	 */
	public static List<SessionParticipant> extractParticipants(AuthenticationResult... results) 
			throws AuthenticationException
	{
		List<SessionParticipant> ret = new ArrayList<>();
		for (AuthenticationResult result: results)
		{
			if (result.getRemoteAuthnContext() != null && 
					result.getRemoteAuthnContext().getSessionParticipants() != null)
				ret.addAll(result.getRemoteAuthnContext().getSessionParticipants());
		}
		return ret;
	}
}
