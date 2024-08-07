/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.remote;

import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupHandler;
import io.imunity.vaadin.endpoint.common.api.RemoteRegistrationSignupResolverFactory;
import io.imunity.vaadin.endpoint.common.forms.ResolvedInvitationParam;
import org.springframework.stereotype.Component;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;

@Component
class RemoteRegistrationSignupResolverFactoryImpl implements RemoteRegistrationSignupResolverFactory
{
	@Override
	public RemoteRegistrationSignupHandler create(AuthenticatorSupportService authnSupport, MessageSource msg,
												  RegistrationForm form, ResolvedInvitationParam invitation, String regCodeProvided)
	{
		return new RemoteRegistrationSignupHandlerImpl(
				authnSupport, msg, form, invitation, regCodeProvided
		);
	}
}
