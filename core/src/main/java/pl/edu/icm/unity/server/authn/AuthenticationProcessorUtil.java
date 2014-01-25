/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.List;

import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;

/**
 * Utility methods processing results of authenticators
 * @author K. Benedyczak
 */
public class AuthenticationProcessorUtil
{
	
	
	/**
	 * Assumes that the input is a complete result of evaluation of all authenticators in a single set.
	 * Returns an authenticated entity or throws an exception when there is a problem.
	 * 
	 * @param results
	 * @return authenticated entity
	 * @throws AuthenticationException
	 */
	public static AuthenticatedEntity processResults(List<AuthenticationResult> results) throws AuthenticationException
	{
		Long entityId = null;
		for (AuthenticationResult result: results)
		{
			if (result.getStatus() != Status.success)
			{
				if (result.getStatus() == Status.unknownRemotePrincipal && results.size() == 1)
					throw new UnknownRemoteUserException("AuthenticationProcessorUtil.authnFailed", 
							result.getFormForUnknownPrincipal(), result.getRemoteAuthnContext());
				throw new AuthenticationException("AuthenticationProcessorUtil.authnFailed");
			}
			Long curId = result.getAuthenticatedEntity().getEntityId();
			if (entityId == null)
				entityId = curId;
			else
				if (!entityId.equals(curId))
				{
					throw new AuthenticationException("AuthenticationProcessorUtil.authnWrongUsers");
				}
		}
		AuthenticatedEntity logInfo = results.get(0).getAuthenticatedEntity();
		for (int i=1; i<results.size(); i++)
			logInfo.getAuthenticatedWith().addAll(
					results.get(i).getAuthenticatedEntity().getAuthenticatedWith());
		return logInfo;
	}

}
