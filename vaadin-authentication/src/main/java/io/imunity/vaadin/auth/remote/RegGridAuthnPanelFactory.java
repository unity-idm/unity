/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.remote;

import io.imunity.vaadin.auth.AuthNOption;
import io.imunity.vaadin.auth.AuthNPanelFactory;
import io.imunity.vaadin.auth.FirstFactorAuthNPanel;
import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.registration.RegistrationForm;

class RegGridAuthnPanelFactory implements AuthNPanelFactory
{
	private final RegistrationForm form;
	private final String regCodeProvided;
	private final boolean enabled;

	RegGridAuthnPanelFactory(RegistrationForm form, String regCodeProvided, boolean enabled)
	{
		this.form = form;
		this.regCodeProvided = regCodeProvided;
		this.enabled = enabled;
	}

	@Override
	public FirstFactorAuthNPanel createRegularAuthnPanel(AuthNOption authnOption)
	{
		return null;
	}

	@Override
	public FirstFactorAuthNPanel createGridCompatibleAuthnPanel(AuthNOption authnOption)
	{
		AuthenticationOptionKey optionId = new AuthenticationOptionKey(
				authnOption.authenticator.getAuthenticatorId(),
				authnOption.authenticatorUI.getId());

		FirstFactorAuthNPanel firstFactorAuthNPanel = new FirstFactorAuthNPanel(null, null, true,
				authnOption.authenticatorUI, optionId);

		if (enabled)
			authnOption.authenticatorUI.setAuthenticationCallback(
					new SignUpAuthnCallback(form, regCodeProvided, optionId));

		return firstFactorAuthNPanel;
	}
}
