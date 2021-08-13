/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.types.registration.RegistrationForm;

public class AuthenticationTriggeringContext
{
	public final boolean rememberMeSet;
	public final PartialAuthnState firstFactorAuthnState;
	public final RegistrationForm form;
	public final String invitationCode;
	public final SandboxAuthnRouter sandboxRouter;

	private AuthenticationTriggeringContext(boolean rememberMeSet,
			PartialAuthnState postFirstFactorAuthnState, RegistrationForm form, 
			String invitationCode,
			SandboxAuthnRouter sandboxRouter)
	{
		this.rememberMeSet = rememberMeSet;
		this.firstFactorAuthnState = postFirstFactorAuthnState;
		this.form = form;
		this.invitationCode = invitationCode;
		this.sandboxRouter = sandboxRouter;
	}
	
	public static AuthenticationTriggeringContext registrationTriggeredAuthn(RegistrationForm form, 
			String invitationCode)
	{
		if (form == null)
			throw new IllegalArgumentException("Form must be set in registration triggered remote authn");
		return new AuthenticationTriggeringContext(false, null, form, invitationCode, null);
	}
	

	public static AuthenticationTriggeringContext authenticationTriggeredFirstFactor(boolean rememberMeSet)
	{
		return new AuthenticationTriggeringContext(rememberMeSet, null, null, null, null);
	}

	public static AuthenticationTriggeringContext authenticationTriggeredFirstFactor()
	{
		return new AuthenticationTriggeringContext(false, null, null, null, null);
	}
	
	public static AuthenticationTriggeringContext authenticationTriggeredSecondFactor(boolean rememberMeSet, 
			PartialAuthnState postFirstFactorAuthnState)
	{
		return new AuthenticationTriggeringContext(rememberMeSet, postFirstFactorAuthnState, null, null, null);
	}

	public static AuthenticationTriggeringContext sandboxTriggeredFirstFactor(SandboxAuthnRouter sandboxRouter)
	{
		if (sandboxRouter == null)
			throw new IllegalArgumentException("Sandbox router must be set in sandbox triggered remote authn");
		return new AuthenticationTriggeringContext(false, null, null, null, sandboxRouter);
	}

	public static AuthenticationTriggeringContext sandboxTriggeredSecondFactor(PartialAuthnState postFirstFactorAuthnState,
			SandboxAuthnRouter sandboxRouter)
	{
		return new AuthenticationTriggeringContext(false, postFirstFactorAuthnState, null, null, sandboxRouter);
	}
	
	public boolean isRegistrationTriggered()
	{
		return form != null;
	}

	public boolean isSandboxTriggered()
	{
		return sandboxRouter != null;
	}

	
	@Override
	public String toString()
	{
		return String.format(
				"AuthenticationTriggeringContext [rememberMeSet=%s, firstFactorAuthnState=%s, form=%s, invitationCode=%s]",
				rememberMeSet, firstFactorAuthnState, form, invitationCode);
	}
}