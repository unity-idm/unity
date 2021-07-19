/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;

public class SignUpAuthnCallback implements AuthenticationCallback
{
	private final RegistrationForm form;
	private final String registrationCode;

	SignUpAuthnCallback(RegistrationForm form, String registrationCode)
	{
		this.form = form;
		this.registrationCode = registrationCode;
	}

	@Override
	public void onStartedAuthentication()
	{
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result)
	{
	}

	@Override
	public void onCancelledAuthentication()
	{
	}

	@Override
	public AuthenticationTriggeringContext getTriggeringContext()
	{
		return AuthenticationTriggeringContext.registrationTriggeredAuthn(form, registrationCode);
	}
}