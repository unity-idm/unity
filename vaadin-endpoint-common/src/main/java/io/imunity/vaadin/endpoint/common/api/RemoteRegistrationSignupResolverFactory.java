/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.endpoint.common.api;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.types.registration.RegistrationForm;
import pl.edu.icm.unity.webui.forms.ResolvedInvitationParam;

public interface RemoteRegistrationSignupResolverFactory
{
	RemoteRegistrationSignupResolver create(AuthenticatorSupportService authnSupport, MessageSource msg,
	                                        RegistrationForm form, ResolvedInvitationParam invitation, String regCodeProvided);
}
