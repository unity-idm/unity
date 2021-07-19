/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn.remote;

import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.types.registration.RegistrationForm;

public class AuthenticationTriggeringContext
{
	public final boolean rememberMeSet;
	public final PartialAuthnState firstFactorAuthnState;
	public final RegistrationForm form;

	private AuthenticationTriggeringContext(boolean rememberMeSet,
			PartialAuthnState postFirstFactorAuthnState, RegistrationForm form)
	{
		this.rememberMeSet = rememberMeSet;
		this.firstFactorAuthnState = postFirstFactorAuthnState;
		this.form = form;
	}
	
	public static AuthenticationTriggeringContext registrationTriggeredAuthn(RegistrationForm form)
	{
		if (form == null)
			throw new IllegalArgumentException("Form must be set in registration triggered remote authn");
		return new AuthenticationTriggeringContext(false, null, form);
	}
	
	public static AuthenticationTriggeringContext authenticationTriggeredFirstFactor(boolean rememberMeSet)
	{
		return new AuthenticationTriggeringContext(rememberMeSet, null, null);
	}
	
	public static AuthenticationTriggeringContext authenticationTriggeredSecondFactor(boolean rememberMeSet, 
			PartialAuthnState postFirstFactorAuthnState)
	{
		return new AuthenticationTriggeringContext(rememberMeSet, postFirstFactorAuthnState, null);
	}

	public boolean isRegistrationTriggered()
	{
		return form != null;
	}

	@Override
	public String toString()
	{
		return String.format(
				"AuthenticationTriggeringContext [rememberMeSet=%s, firstFactorAuthnState=%s, form=%s]",
				rememberMeSet, firstFactorAuthnState, form);
	}
}