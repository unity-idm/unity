/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.remote;

import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupResolver;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupResolverFactory;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

@Component
class RemoteRegistrationSignupResolverFactoryImpl implements RemoteRegistrationSignupResolverFactory
{
	@Override
	public RemoteRegistrationSignupResolver create(AuthenticatorSupportService authnSupport, MessageSource msg,
	                                               RegistrationForm form, ResolvedInvitationParam invitation, String regCodeProvided)
	{
		return new RemoteRegistrationSignupResolverImpl(
				authnSupport, msg, form, invitation, regCodeProvided
		);
	}
}
