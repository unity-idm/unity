/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.authn;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

import org.apache.logging.log4j.Logger;
import org.mvel2.MVEL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
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
import pl.edu.icm.unity.engine.credential.CredentialRepository;
import pl.edu.icm.unity.engine.identity.SecondFactorOptInService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;
import pl.edu.icm.unity.types.authn.CredentialDefinition;
import pl.edu.icm.unity.types.authn.DynamicExpressionPolicyConfiguration;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Utility methods processing results of authenticators.
 * 
 * @author K. Benedyczak
 */
@Component
class AuthenticationProcessorImpl implements AuthenticationProcessor
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, AuthenticationProcessorImpl.class);

	private final SecondFactorOptInService secondFactorOptInService;
	private final LocalCredentialsRegistry localCred;
	private final CredentialRepository credRepo;
	private final AuthenticationFlowPolicyConfigMVELContextBuilder policyConfigMVELContextBuilder;

	@Autowired
	AuthenticationProcessorImpl(SecondFactorOptInService secondFactorOptInService, LocalCredentialsRegistry localCred,
			CredentialRepository credRepo,
			AuthenticationFlowPolicyConfigMVELContextBuilder policyConfigMVELContextBuilder)
	{
		this.secondFactorOptInService = secondFactorOptInService;
		this.localCred = localCred;
		this.credRepo = credRepo;
		this.policyConfigMVELContextBuilder = policyConfigMVELContextBuilder;
	}

	/**
	 * Starting point: the result of the primary authenticator is verified. If the
	 * authentication failed then an exception is thrown. Otherwise it is checked
	 * whether, according to the {@link AuthenticationFlow} selected, second
	 * authentication should be performed, what is returned.
	 */
	@Override
	public PartialAuthnState processPrimaryAuthnResult(AuthenticationResult result,
			AuthenticationFlow authenticationFlow, AuthenticationOptionKey authnOptionId) throws AuthenticationException
	{
		if (result.getStatus() != Status.success)
		{
			if (result.getStatus() == Status.unknownRemotePrincipal)
				throw new UnknownRemoteUserException("AuthenticationProcessorImpl.authnFailed", result.asRemote());
			throw new AuthenticationException("AuthenticationProcessorImpl.authnFailed");
		}

		Policy flowPolicy = authenticationFlow.getPolicy();
		if (flowPolicy.equals(Policy.REQUIRE))
		{
			return proccessRequiredPolicy(result, authenticationFlow, authnOptionId);
		} else if (flowPolicy.equals(Policy.USER_OPTIN))
		{
			Optional<PartialAuthnState> retOfUserOptIn = proccessUserOptInPolicy(result, authenticationFlow,
					authnOptionId);
			if (retOfUserOptIn.isPresent())
				return retOfUserOptIn.get();

		} else if (flowPolicy.equals(Policy.DYNAMIC_EXPRESSION))
		{
			Optional<PartialAuthnState> retOfDynamicPolicy = proccessDynamicPolicy(result, authenticationFlow,
					authnOptionId);
			if (retOfDynamicPolicy.isPresent())
				return retOfDynamicPolicy.get();
		}

		return new PartialAuthnState(authnOptionId, null, result, authenticationFlow);
	}

	private PartialAuthnState proccessRequiredPolicy(AuthenticationResult result, AuthenticationFlow authenticationFlow,
			AuthenticationOptionKey authnOptionId) throws AuthenticationException
	{
		PartialAuthnState partialAuthnState = getSecondFactorAuthn(authenticationFlow, result, authnOptionId);
		if (partialAuthnState != null)
			return partialAuthnState;

		throw new AuthenticationException("AuthenticationProcessorImpl.secondFactorRequire");
	}

	private Optional<PartialAuthnState> proccessUserOptInPolicy(AuthenticationResult result,
			AuthenticationFlow authenticationFlow, AuthenticationOptionKey authnOptionId) throws AuthenticationException
	{
		PartialAuthnState partialAuthnState = null;
		if (getUserOptInAttribute(result.getSuccessResult().authenticatedEntity.getEntityId()))
		{
			partialAuthnState = getSecondFactorAuthn(authenticationFlow, result, authnOptionId);

			if (partialAuthnState != null)
				return Optional.of(partialAuthnState);

			throw new AuthenticationException("AuthenticationProcessorImpl.secondFactorRequire");
		}
		return Optional.empty();
	}

	private Optional<PartialAuthnState> proccessDynamicPolicy(AuthenticationResult result,
			AuthenticationFlow authenticationFlow, AuthenticationOptionKey authnOptionId) throws AuthenticationException
	{
		PartialAuthnState partialAuthnState = null;
		try
		{
			String mvelExpression = ((DynamicExpressionPolicyConfiguration) authenticationFlow
					.getPolicyConfiguration()).mvelExpression;
			if (evaluateCondition(mvelExpression,
					policyConfigMVELContextBuilder.createMvelContext(authnOptionId, result,
							getUserOptInAttribute(result.getSuccessResult().authenticatedEntity.getEntityId()),
							authenticationFlow)))
			{
				partialAuthnState = getSecondFactorAuthn(authenticationFlow, result, authnOptionId);

				if (partialAuthnState != null)
					return Optional.of(partialAuthnState);

				throw new AuthenticationException("AuthenticationProcessorImpl.secondFactorRequire");
			}
		} catch (EngineException e)
		{
			throw new AuthenticationException("AuthenticationProcessorImpl.authnFailed");
		}

		return Optional.empty();
	}

	private boolean evaluateCondition(String condition, Object input) throws EngineException
	{
		Serializable compiled = MVEL.compileExpression(condition);

		Boolean result = null;
		try
		{
			result = (Boolean) MVEL.executeExpression(compiled, input, new HashMap<>());
		} catch (Exception e)
		{
			log.warn("Error during expression execution.", e);
			throw new EngineException(e);
		}

		if (result == null)
		{
			log.debug("Condition evaluated to null value, assuming false");
			return false;
		}

		log.debug("Condition \"{}\" evaluated to {}", condition, result.booleanValue());
		return result.booleanValue();
	}

	private boolean getUserOptInAttribute(long entityId)
	{
		try
		{
			return secondFactorOptInService.getUserOptin(entityId);
		} catch (EngineException e)
		{
			log.debug("Can not get user optin attribute for entity " + entityId);
			// force second factor
			return true;
		}
	}

	private PartialAuthnState getSecondFactorAuthn(AuthenticationFlow authenticationFlow, AuthenticationResult result,
			AuthenticationOptionKey firstFactorauthnOptionId)
	{
		if (result.getSuccessResult().authenticatedEntity == null)
			return null;
		AuthenticatorInstance secondFactorAuthenticator = getValidAuthenticatorForEntity(
				authenticationFlow.getSecondFactorAuthenticators(),
				result.getSuccessResult().authenticatedEntity.getEntityId());
		if (secondFactorAuthenticator == null)
			return null;
		return new PartialAuthnState(firstFactorauthnOptionId, secondFactorAuthenticator.getRetrieval(), result,
				authenticationFlow);
	}

	@Override
	public AuthenticatorInstance getValidAuthenticatorForEntity(Collection<AuthenticatorInstance> pool, long entityId)
	{
		for (AuthenticatorInstance authn : pool)
		{
			AuthenticatorInstanceMetadata authenticator = authn.getMetadata();
			if (authenticator != null)
			{
				if (!authenticator.getTypeDescription()
						.isLocal())
					return authn;
				else if (checkIfUserHasCredential(authenticator, entityId))
					return authn;
			}
		}
		return null;
	}

	private boolean checkIfUserHasLocalCredential(long entityId, String credentialId)
			throws IllegalCredentialException, EngineException
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
			log.debug("Check if user {} has defined credential {}: {}", entityId, authn.getLocalCredentialName(), ret);
			return ret;
		} catch (Exception e)
		{
			log.warn("Can not check entity local credential state", e);
			return false;
		}

	}

	@Override
	public AuthenticatedEntity finalizeAfterPrimaryAuthentication(PartialAuthnState state, boolean skipSecondFactor)
	{
		if (state.isSecondaryAuthenticationRequired() && !skipSecondFactor)
			throw new IllegalStateException(
					"BUG: code tried to finalize authentication " + "requiring MFA after first authentication");
		return state.getPrimaryResult()
				.getSuccessResult().authenticatedEntity;
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

		Long secondId = result2.getSuccessResult().authenticatedEntity.getEntityId();
		AuthenticatedEntity firstAuthenticated = state.getPrimaryResult()
				.getSuccessResult().authenticatedEntity;
		Long primaryId = firstAuthenticated.getEntityId();
		if (!secondId.equals(primaryId))
		{
			throw new AuthenticationException("AuthenticationProcessorImpl.authnWrongUsers");
		}
		AuthenticatedEntity logInfo = result2.getSuccessResult().authenticatedEntity;
		logInfo.getAuthenticatedWith()
				.addAll(firstAuthenticated.getAuthenticatedWith());
		if (firstAuthenticated.getOutdatedCredentialId() != null)
			logInfo.setOutdatedCredentialId(firstAuthenticated.getOutdatedCredentialId());
		return logInfo;
	}
}
