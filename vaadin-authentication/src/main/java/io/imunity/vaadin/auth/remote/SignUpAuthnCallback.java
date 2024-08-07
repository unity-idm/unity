/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.remote;

import io.imunity.vaadin.auth.VaadinAuthentication;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationRetrievalContext;
import pl.edu.icm.unity.engine.api.authn.remote.AuthenticationTriggeringContext;

public class SignUpAuthnCallback implements VaadinAuthentication.AuthenticationCallback
{
	private final RegistrationForm form;
	private final String registrationCode;
	private final AuthenticationOptionKey authnOptionKey;

	SignUpAuthnCallback(RegistrationForm form, String registrationCode, AuthenticationOptionKey authnOptionKey)
	{
		this.form = form;
		this.registrationCode = registrationCode;
		this.authnOptionKey = authnOptionKey;
	}

	@Override
	public void onStartedAuthentication()
	{
	}

	@Override
	public void onCompletedAuthentication(AuthenticationResult result, AuthenticationRetrievalContext retrievalContext)
	{
	}

	@Override
	public void onCancelledAuthentication()
	{
	}

	@Override
	public AuthenticationTriggeringContext getTriggeringContext()
	{
		return AuthenticationTriggeringContext.registrationTriggeredAuthn(form, registrationCode, authnOptionKey);
	}
}