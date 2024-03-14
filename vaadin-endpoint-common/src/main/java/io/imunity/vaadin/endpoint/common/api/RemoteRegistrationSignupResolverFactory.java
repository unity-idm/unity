/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import io.imunity.vaadin.endpoint.common.forms.ResolvedInvitationParam;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.registration.RegistrationForm;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;

public interface RemoteRegistrationSignupResolverFactory
{
	RemoteRegistrationSignupHandler create(AuthenticatorSupportService authnSupport, MessageSource msg,
										   RegistrationForm form, ResolvedInvitationParam invitation, String regCodeProvided);
}
