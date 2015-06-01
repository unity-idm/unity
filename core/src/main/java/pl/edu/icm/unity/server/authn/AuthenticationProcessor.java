/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.api.internal.SessionParticipant;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;

/**
 * Utility methods processing results of authenticators.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticationProcessor
{
	/**
	 * Starting point: the result of the primary authenticator is verified. If the authentication failed
	 * then an exception is thrown. Otherwise it is checked whether, according to the 
	 * {@link AuthenticationOption} selected, second authentication should be performed, what is returned.
	 * @param result
	 * @param authenticationOption
	 * @return
	 * @throws AuthenticationException
	 */
	public PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result, 
			AuthenticationOption authenticationOption) throws AuthenticationException
	{
		if (result.getStatus() != Status.success)
		{
			if (result.getStatus() == Status.unknownRemotePrincipal)
				throw new UnknownRemoteUserException("AuthenticationProcessorUtil.authnFailed", 
						result);
			throw new AuthenticationException("AuthenticationProcessorUtil.authnFailed");
		}
		
		//in future RBA or advanced (e.g. user driven MFA) will be activated here.
		
		if (authenticationOption.getMandatory2ndAuthenticator() != null)
			return new PartialAuthnStateImpl(authenticationOption.getMandatory2ndAuthenticator(), result);
		else
			return new PartialAuthnStateImpl(null, result);
	}

	/**
	 * Should be used if the second step authentication is required to process second authenticator results
	 * and retrieve a final {@link AuthenticatedEntity}.
	 * @param state
	 * @return
	 */
	public AuthenticatedEntity finalizeAfterPrimaryAuthentication(PartialAuthnState state)
	{
		if (state.isSecondaryAuthenticationRequired())
			throw new IllegalStateException("BUG: code tried to finalize authentication "
					+ "requiring MFA after first authentication");
		return state.getPrimaryResult().getAuthenticatedEntity();
	}

	
	/**
	 * Should be used if the second step authentication is required to process second authenticator results
	 * and retrieve a final {@link AuthenticatedEntity}.
	 * @param primaryResult
	 * @return
	 * @throws AuthenticationException 
	 */
	public AuthenticatedEntity finalizeAfterSecondaryAuthentication(PartialAuthnState state, 
			AuthenticationResult result2) throws AuthenticationException
	{
		if (!state.isSecondaryAuthenticationRequired())
			throw new IllegalStateException("BUG: code tried to finalize authentication "
					+ "with additional authentication while only one was selected");
		
		if (result2.getStatus() != Status.success)
		{
			if (result2.getStatus() == Status.unknownRemotePrincipal)
				throw new AuthenticationException("AuthenticationProcessorUtil.authnWrongUsers");
			throw new AuthenticationException("AuthenticationProcessorUtil.authnFailed");
		}
		
		Long secondId = result2.getAuthenticatedEntity().getEntityId();
		AuthenticatedEntity firstAuthenticated = state.getPrimaryResult().getAuthenticatedEntity(); 
		Long primaryId = firstAuthenticated.getEntityId();
		if (!secondId.equals(primaryId))
		{
			throw new AuthenticationException("AuthenticationProcessorUtil.authnWrongUsers");
		}
		AuthenticatedEntity logInfo = result2.getAuthenticatedEntity();
		logInfo.getAuthenticatedWith().addAll(firstAuthenticated.getAuthenticatedWith());
		return logInfo;
	}
	
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
	
	/**
	 * Provides information about partial state of authentication. Basing on the contents the 
	 * framework should perform additional authentication or proceed to establish final authentication result.
	 * @author K. Benedyczak
	 */
	public interface PartialAuthnState
	{
		boolean isSecondaryAuthenticationRequired();
		BindingAuthn getSecondaryAuthenticator();
		AuthenticationResult getPrimaryResult();
	}
	
	private class PartialAuthnStateImpl implements PartialAuthnState
	{
		private BindingAuthn secondaryAuthenticator;
		private AuthenticationResult primaryResult;

		public PartialAuthnStateImpl(BindingAuthn secondaryAuthenticator,
				AuthenticationResult result)
		{
			this.secondaryAuthenticator = secondaryAuthenticator;
			this.primaryResult = result;
		}

		@Override
		public boolean isSecondaryAuthenticationRequired()
		{
			return secondaryAuthenticator != null;
		}

		@Override
		public BindingAuthn getSecondaryAuthenticator()
		{
			return secondaryAuthenticator;
		}

		@Override
		public AuthenticationResult getPrimaryResult()
		{
			return primaryResult;
		}
	}
}
