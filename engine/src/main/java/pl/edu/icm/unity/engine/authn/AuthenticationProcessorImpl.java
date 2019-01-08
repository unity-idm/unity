/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.AuthenticationFlowManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialsRegistry;
import pl.edu.icm.unity.engine.api.authn.remote.UnknownRemoteUserException;
import pl.edu.icm.unity.engine.api.session.SessionParticipant;
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;
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
	private static final Logger log = Log.getLogger(Log.U_SERVER, AuthenticationProcessorImpl.class);
	
	private AuthenticationFlowManagement authFlowMan;
	private LocalCredentialsRegistry localCred;
	private CredentialRepository credRepo;
	
	@Autowired
	public AuthenticationProcessorImpl(
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
	 * @param authnOptionId
	 * @return
	 * @throws AuthenticationException
	 */
	@Override
	public PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result, 
			AuthenticationFlow authenticationFlow, String authnOptionId) throws AuthenticationException
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
					authenticationFlow, result, authnOptionId);
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
						result, authnOptionId);

				if (partialAuthnState != null)
					return partialAuthnState;

				throw new AuthenticationException(
						"AuthenticationProcessorImpl.secondFactorRequire");
			}

		}
		// In Future: Risk base policy

		return new PartialAuthnState(authnOptionId, null, result, authenticationFlow);
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

	private PartialAuthnState getSecondFactorAuthn(AuthenticationFlow authenticationFlow, 
			AuthenticationResult result, String firstFactorauthnOptionId)
	{
		AuthenticatorInstance secondFactorAuthenticator = getValidAuthenticatorForEntity(
				authenticationFlow.getSecondFactorAuthenticators(), 
				result.getAuthenticatedEntity().getEntityId());
		if (secondFactorAuthenticator == null)
			return null;
		return new PartialAuthnState(firstFactorauthnOptionId, secondFactorAuthenticator.getRetrieval(), 
				result, authenticationFlow);
	}
	
	@Override
	public AuthenticatorInstance getValidAuthenticatorForEntity(Collection<AuthenticatorInstance> pool, long entityId)
	{
		for (AuthenticatorInstance authn : pool)
		{
			AuthenticatorInstanceMetadata authenticator = authn.getMetadata();
			if (authenticator != null)
			{
				if (!authenticator.getTypeDescription().isLocal())
				{
					log.debug("Using remote second factor authenticator " + authenticator.getId());
					return authn;

				} else if (checkIfUserHasCredential(authenticator, entityId))
				{
					log.debug("Using local second factor authenticator " + authenticator.getId());
					return authn;
				}
			}
		}
		return null;
	}
	
	
	private boolean checkIfUserHasLocalCredential(long entityId,
			String credentialId) throws IllegalCredentialException, EngineException
	{

		CredentialDefinition credentialDefinition = credRepo.get(credentialId);
		return localCred.createLocalCredentialVerificator(credentialDefinition)
				.isCredentialSet(new EntityParam(entityId));
	}
	
	@Override
	public boolean checkIfUserHasCredential(AuthenticatorInstanceMetadata authn, long entityId)
	{
		
		try
		{
			boolean ret = checkIfUserHasLocalCredential(entityId, authn.getLocalCredentialName());
			log.debug("Check if user {} has defined credential {}: {}", 
					entityId, authn.getLocalCredentialName(), ret);
			return ret;
		} catch (Exception e)
		{
			log.debug("Can not check entity local credential state", e);
			return false;
		}

	}
		
	@Override
	public AuthenticatedEntity finalizeAfterPrimaryAuthentication(PartialAuthnState state, boolean skipSecondFactor)
	{
		if (state.isSecondaryAuthenticationRequired() && !skipSecondFactor)
			throw new IllegalStateException("BUG: code tried to finalize authentication "
					+ "requiring MFA after first authentication");
		return state.getPrimaryResult().getAuthenticatedEntity();
	}

	
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
		if (firstAuthenticated.getOutdatedCredentialId() != null)
			logInfo.setOutdatedCredentialId(firstAuthenticated.getOutdatedCredentialId());
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
