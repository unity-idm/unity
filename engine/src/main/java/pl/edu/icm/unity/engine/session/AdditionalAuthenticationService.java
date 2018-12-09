/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.session;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorInstance;
import pl.edu.icm.unity.engine.api.authn.InvocationContext;
import pl.edu.icm.unity.engine.api.authn.LoginSession;
import pl.edu.icm.unity.engine.api.authn.LoginSession.AuthNInfo;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationMisconfiguredException;
import pl.edu.icm.unity.engine.api.session.AdditionalAuthenticationRequiredException;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKeyUtils;
import pl.edu.icm.unity.types.authn.AuthenticatorInstanceMetadata;

/**
 * Establishes whether a sensitive operation should be protected by a repeated or step-up authentication.
 * 
 * @author K. Benedyczak
 */
@Component
public class AdditionalAuthenticationService
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, AdditionalAuthenticationService.class);
	private final AuthenticationProcessor authnProcessor;
	private final String policyStr;
	private final boolean failOnNoMatch;
	private final long graceTimeMS;
	
	@Autowired
	public AdditionalAuthenticationService(UnityServerConfiguration config, AuthenticationProcessor authnProcessor)
	{
		this(authnProcessor, 
				config.getValue(UnityServerConfiguration.RE_AUTHENTICATION_POLICY), 
				config.getBooleanValue(UnityServerConfiguration.RE_AUTHENTICATION_BLOCK_ON_NONE), 
				config.getIntValue(UnityServerConfiguration.RE_AUTHENTICATION_GRACE_TIME) * 1000);
	}

	AdditionalAuthenticationService(AuthenticationProcessor authnProcessor,
			String policy, boolean failOnNoMatch, long graceTimeMS)
	{
		this.policyStr = policy;
		this.failOnNoMatch = failOnNoMatch;
		this.graceTimeMS = graceTimeMS;
		this.authnProcessor = authnProcessor;
	}

	public void checkAdditionalAuthenticationRequirements()
	{
		checkAdditionalAuthenticationRequirements(null);
	}

	public void checkAdditionalAuthenticationRequirements(String modifiedCredential)
	{
		String additionalAuthnOption = getOptionToReAuthenticate(Optional.ofNullable(modifiedCredential));
		boolean additionalAuthnRequired = additionalAuthnOption != null &&
				isAdditionalAuthnRequiredForOption(additionalAuthnOption);
		if (!additionalAuthnRequired)
			return;
		
		log.debug("Additional authn is required with option {}", additionalAuthnOption);
		throw new AdditionalAuthenticationRequiredException(additionalAuthnOption);
	}

	private String getOptionToReAuthenticate(Optional<String> modifiedCredential)
	{
		String[] policyElements = policyStr.trim().split("[ ]+");
		for (String policyElement: policyElements)
		{
			String option = null;
			switch (policyElement)
			{
			case "ENDPOINT_2F":
				option = getEndpoint2ndF();
				break;
			case "SESSION_1F":
				option = getSession1stF();
				break;
			case "SESSION_2F":
				option = getSession2ndF();
				break;
			case "CURRENT":
				option = getMatchingCredential(modifiedCredential);
				break;
			default:
				option = findOnEndpoint(policyElement);
			}
			log.debug("Trying {} additional authN option from policy, result: {}", policyElement, option);
			
			if (option != null)
				return option;
		}
		
		if (failOnNoMatch)
		{
			log.debug("Additional authn is required but no option was found, blocking operation");
			throw new AdditionalAuthenticationMisconfiguredException();
		}
		return null;
	}


	private String findOnEndpoint(String authenticatorCandidate)
	{
		Optional<AuthenticatorInstance> endpointAuthenticator = getEndpointAuthenticator(authenticatorCandidate);
		if (!endpointAuthenticator.isPresent())
			return null;
		if (isValidForReauthentication(endpointAuthenticator.get()))
			return authenticatorCandidate;
		return null;
	}


	private String getMatchingCredential(Optional<String> modifiedCredential)
	{
		if (!modifiedCredential.isPresent())
			return null;
		String credential = modifiedCredential.get();
		List<AuthenticationFlow> authenticationFlows = InvocationContext.getCurrent().getEndpointFlows();
		for (AuthenticationFlow flow: authenticationFlows)
		{
			Set<AuthenticatorInstance> allAuthenticators = flow.getAllAuthenticators();
			for (AuthenticatorInstance auth: allAuthenticators)
			{
				String authId = auth.getMetadata().getId();
				if (credential.equals(auth.getMetadata().getLocalCredentialName()) &&
						isValidForReauthentication(auth))
					return authId;
			}
		}
		return null;
	}

	private String getSession1stF()
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		return getFromSessionFactor(loginSession.getLogin1stFactorOptionId());
	}

	private String getSession2ndF()
	{
		LoginSession loginSession = InvocationContext.getCurrent().getLoginSession();
		return getFromSessionFactor(loginSession.getLogin2ndFactorOptionId());
	}

	private String getFromSessionFactor(String loginFactor)
	{
		if (loginFactor != null)
		{
			String authenticator = AuthenticationOptionKeyUtils.decodeAuthenticator(loginFactor);
			if(isValidForReauthentication(authenticator))
				return authenticator;
		}
		return null;
	}
	

	private String getEndpoint2ndF()
	{
		List<AuthenticationFlow> authenticationFlows = InvocationContext.getCurrent().getEndpointFlows();
		for (AuthenticationFlow flow: authenticationFlows)
		{
			List<AuthenticatorInstance> authenticators = flow.getSecondFactorAuthenticators();
			long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
			AuthenticatorInstance authenticator = authnProcessor.getValidAuthenticatorForEntity(authenticators, entityId);
			if (authenticator != null)
				return authenticator.getMetadata().getId();
		}
		return null;
	}

	
	
	private Optional<AuthenticatorInstance> getEndpointAuthenticator(String name)
	{
		List<AuthenticationFlow> authenticationFlows = InvocationContext.getCurrent().getEndpointFlows();
		return authenticationFlows.stream()
			.flatMap(flow -> flow.getAllAuthenticators().stream())
			.filter(a -> name.equals(a.getMetadata().getId()))
			.findAny();
	}

	private boolean isValidForReauthentication(String authnOption)
	{
		Optional<AuthenticatorInstance> endpointAuthenticator = getEndpointAuthenticator(authnOption);
		if (!endpointAuthenticator.isPresent())
			return false;
		AuthenticatorInstance authenticator = endpointAuthenticator.get();
		return isValidForReauthentication(authenticator);
	}
	
	private boolean isValidForReauthentication(AuthenticatorInstance authenticator)
	{
		if (authenticator.getRetrieval().requiresRedirect())
			return false;
		if (authenticator.getMetadata().getLocalCredentialName() == null)
			return false;
		return userCanUse(authenticator.getMetadata());
	}
	
	private boolean userCanUse(AuthenticatorInstanceMetadata authn)
	{
		long entityId = InvocationContext.getCurrent().getLoginSession().getEntityId();
		return authnProcessor.checkIfUserHasCredential(authn, entityId);
	}

	private boolean isAdditionalAuthnRequiredForOption(String additionalAuthnOption)
	{
		LoginSession session = InvocationContext.getCurrent().getLoginSession();
		
		if (checkAuthnInfoInGracePeriod(session.getAdditionalAuthn(), additionalAuthnOption, graceTimeMS))
			return false;
		if (checkAuthnInfoInGracePeriod(session.getLogin1stFactor(), additionalAuthnOption, graceTimeMS))
			return false;
		if (checkAuthnInfoInGracePeriod(session.getLogin2ndFactor(), additionalAuthnOption, graceTimeMS))
			return false;
		return true;
	}
	
	private boolean checkAuthnInfoInGracePeriod(AuthNInfo authnInfo, String expectedAuthnOption, long graceTime)
	{
		log.trace("Checking if {} contains {} in grace period {} at {}", authnInfo, 
				expectedAuthnOption, graceTime, System.currentTimeMillis());
		if (authnInfo == null || authnInfo.optionId == null)
			return false;
		String authenticator = AuthenticationOptionKeyUtils.decodeAuthenticator(authnInfo.optionId);
		if (!authenticator.equals(expectedAuthnOption))
			return false;
		return System.currentTimeMillis() < graceTime + authnInfo.time.getTime();
	}
}
