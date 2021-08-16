/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import java.util.Date;
import java.util.function.Function;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext.FactorOrder;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;

/**
 * Base class for storing some context information related to external login using a redirect.
 */
public class RedirectedAuthnState extends RelayedAuthnState
{
	private final AuthenticationStepContext authenticationContext;
	private final LoginMachineDetails initialLoginMachine;
	private final String ultimateReturnURL;
	private final AuthenticationTriggeringContext authenticationTriggeringContext;
	private final Function<RedirectedAuthnState, AuthenticationResult> responseHandler;
	
	private RedirectedAuthnState(AuthenticationStepContext authenticationContext, 
			Function<RedirectedAuthnState, AuthenticationResult> responseHandler,
			LoginMachineDetails initialLoginMachine, 
			String ultimateReturnURL,
			AuthenticationTriggeringContext authenticationTriggeringContext,
			String relayState,
			Date creationTime)
	{
		super(relayState, creationTime);
		if (authenticationTriggeringContext.firstFactorAuthnState != null 
				&& authenticationContext.factor == FactorOrder.FIRST)
			throw new IllegalArgumentException("Can't set first factor result for the first factor state");
		if (authenticationTriggeringContext.firstFactorAuthnState == null 
				&& authenticationContext.factor == FactorOrder.SECOND)
			throw new IllegalArgumentException("Must set first factor result for the second factor state");
		this.authenticationContext = authenticationContext;
		this.responseHandler = responseHandler;
		this.initialLoginMachine = initialLoginMachine;
		this.ultimateReturnURL = ultimateReturnURL;
		this.authenticationTriggeringContext = authenticationTriggeringContext;
	}

	public RedirectedAuthnState(AuthenticationStepContext authenticationContext, 
			Function<RedirectedAuthnState, AuthenticationResult> responseHandler,
			LoginMachineDetails initialLoginMachine, 
			String ultimateReturnURL,
			AuthenticationTriggeringContext authenticationTriggeringContext)
	{
		if (authenticationTriggeringContext.firstFactorAuthnState != null 
				&& authenticationContext.factor == FactorOrder.FIRST)
			throw new IllegalArgumentException("Can't set first factor result for the first factor state");
		if (authenticationTriggeringContext.firstFactorAuthnState == null 
				&& authenticationContext.factor == FactorOrder.SECOND)
			throw new IllegalArgumentException("Must set first factor result for the second factor state");
		this.authenticationContext = authenticationContext;
		this.responseHandler = responseHandler;
		this.initialLoginMachine = initialLoginMachine;
		this.ultimateReturnURL = ultimateReturnURL;
		this.authenticationTriggeringContext = authenticationTriggeringContext;
	}

	public RedirectedAuthnState(RedirectedAuthnState toCopy)
	{
		this(toCopy.authenticationContext, toCopy.responseHandler, 
				toCopy.initialLoginMachine, 
				toCopy.ultimateReturnURL, 
				toCopy.authenticationTriggeringContext,
				toCopy.getRelayState(),
				toCopy.getCreationTime());
	}
	
	public AuthenticationTriggeringContext getAuthenticationTriggeringContext()
	{
		return authenticationTriggeringContext;
	}

	public AuthenticationStepContext getAuthenticationStepContext() 
	{
		return authenticationContext;
	}

	public LoginMachineDetails getInitialLoginMachine()
	{
		return initialLoginMachine;
	}

	public String getUltimateReturnURL()
	{
		return ultimateReturnURL;
	}

	public AuthenticationResult processAnswer()
	{
		return responseHandler.apply(this);
	}
}