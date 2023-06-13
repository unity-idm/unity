/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.column;

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