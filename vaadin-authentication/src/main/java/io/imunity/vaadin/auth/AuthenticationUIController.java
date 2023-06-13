/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * Allows for controlling wrapped {@link VaadinAuthenticationUI}
 */
interface AuthenticationUIController
{
	void cancel();
	boolean focusIfPossible();
	AuthenticationOptionKey getAuthenticationOptionId();
}