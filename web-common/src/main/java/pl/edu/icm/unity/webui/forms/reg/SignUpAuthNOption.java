/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.forms.reg;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

class SignUpAuthNOption
{
	final AuthenticationFlow flow;
	final VaadinAuthenticationUI authenticatorUI;

	SignUpAuthNOption(AuthenticationFlow flow, VaadinAuthenticationUI authenticatorUI)
	{
		this.flow = flow;
		this.authenticatorUI = authenticatorUI;
	}
}
