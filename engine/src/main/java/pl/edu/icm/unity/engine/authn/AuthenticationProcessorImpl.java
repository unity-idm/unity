/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.EntityCredentialManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.Authenticator;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Utility methods processing results of authenticators.
 * 
 * @author K. Benedyczak
 */
@Component
public class AuthenticationProcessorImpl implements AuthenticationProcessor
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER, AuthenticationProcessorImpl.class);
	
	private AuthenticationFlowManagement authFlowMan;
	private LocalCredentialsRegistry localCred;
	private CredentialRepository credRepo;
	
	@Autowired
	public AuthenticationProcessorImpl(
			@Qualifier("insecure") EntityCredentialManagement entityCredMan,
			AuthenticationFlowManagement authFlowMan,
			LocalCredentialsRegistry localCred, CredentialRepository credRepo)
	{
		this.authFlowMan = authFlowMan;
		this.localCred = localCred;
		this.credRepo = credRepo;
	}
	
	/**
	 * Starting point: the result of the primary authenticator is verified. If the authentication failed
	 * then an exception is thrown. Otherwise it is checked whether, according to the 
	 * {@link AuthenticationFlow} selected, second authentication should be performed, what is returned.
	 * @param result
	 * @param authenticationFlow
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	public PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result, 
			AuthenticationFlow authenticationFlow) throws AuthenticationException
	{
		if (result.getStatus() != Status.success)
		{
			if (result.getStatus() == Status.unknownRemotePrincipal)
				throw new UnknownRemoteUserException("AuthenticationProcessorImpl.authnFailed", 
						result);
			throw new AuthenticationException("AuthenticationProcessorImpl.authnFailed");
		}
		
		
		Policy flowPolicy = authenticationFlow.getPolicy();
		if (flowPolicy.equals(Policy.REQUIRE))
		{
			PartialAuthnState partialAuthnState = getSecondFactorAuthn(
					authenticationFlow, result);
			if (partialAuthnState != null)
				return partialAuthnState;

			throw new AuthenticationException(
					"AuthenticationProcessorImpl.secondFactorRequire");

		} else if (flowPolicy.equals(Policy.USER_OPTIN))
		{

			PartialAuthnState partialAuthnState = null;
			if (getUserOptInAttribute(result.getAuthenticatedEntity().getEntityId()))
			{
				partialAuthnState = getSecondFactorAuthn(authenticationFlow,
						result);

				if (partialAuthnState != null)
					return partialAuthnState;

				throw new AuthenticationException(
						"AuthenticationProcessorImpl.secondFactorRequire");
			}

		}
		// In Future: Risk base policy

		return new PartialAuthnState(null, result);
	}

	
	private boolean getUserOptInAttribute(long entityId)
	{
		try
		{
			return authFlowMan.getUserMFAOptIn(entityId);
		} catch (EngineException e)
		{
			log.debug("Can not get user optin attribute for entity " + entityId);
			//force second factor
			return true;
		}
	}

	private PartialAuthnState getSecondFactorAuthn(AuthenticationFlow authenticationFlow, AuthenticationResult result)
	{
		for (Authenticator authn : authenticationFlow
				.getSecondFactorAuthenticators())

		{
			
			BindingAuthn bindingAuthn = authn.getRetrieval();
			AuthenticatorInstance authenticator = authn.getAuthenticatorInstance();
			if (authenticator != null)
			{
				if (!authenticator.getTypeDescription().isLocal())
				{
					log.debug("Using remote second factor authenticator " + authenticator.getId());
					return new PartialAuthnState(bindingAuthn, result);

				} else if (checkIfUserHasCredential(authenticator,
						result.getAuthenticatedEntity()))
				{
					log.debug("Using local second factor authenticator " + authenticator.getId());
					return  new PartialAuthnState(bindingAuthn, result);

				}
			}
		}
	
		return null;
	}
	
	
	private boolean checkIfUserHasLocalCredential(AuthenticatedEntity entity,
			String credentialId) throws IllegalCredentialException, EngineException
	{

		CredentialDefinition credentialDefinition = credRepo.get(credentialId);
		return localCred.createLocalCredentialVerificator(credentialDefinition)
				.isCredentialSet(new EntityParam(entity.getEntityId()));
	}
	
	
	private boolean checkIfUserHasCredential(AuthenticatorInstance authn, AuthenticatedEntity entity)
	{
		
		log.debug("Check if user have defined " + authn.getLocalCredentialName() + " credential");
		try
		{
			return checkIfUserHasLocalCredential(entity,
					authn.getLocalCredentialName());
			
		} catch (Exception e)
		{
			log.debug("Can not check entity local credential state", e);
			return false;
		}

	}
		
	/**
	 * Should be used if the second step authentication is required to process second authenticator results
	 * and retrieve a final {@link AuthenticatedEntity}.
	 * @param state
	 * @return
	 */
	@Override
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
	@Override
	public AuthenticatedEntity finalizeAfterSecondaryAuthentication(PartialAuthnState state, 
			AuthenticationResult result2) throws AuthenticationException
	{
		if (!state.isSecondaryAuthenticationRequired())
			throw new IllegalStateException("BUG: code tried to finalize authentication "
					+ "with additional authentication while only one was selected");
		
		if (result2.getStatus() != Status.success)
		{
			if (result2.getStatus() == Status.unknownRemotePrincipal)
				throw new AuthenticationException("AuthenticationProcessorImpl.authnWrongUsers");
			throw new AuthenticationException("AuthenticationProcessorImpl.authnFailed");
		}
		
		Long secondId = result2.getAuthenticatedEntity().getEntityId();
		AuthenticatedEntity firstAuthenticated = state.getPrimaryResult().getAuthenticatedEntity(); 
		Long primaryId = firstAuthenticated.getEntityId();
		if (!secondId.equals(primaryId))
		{
			throw new AuthenticationException("AuthenticationProcessorImpl.authnWrongUsers");
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
}
