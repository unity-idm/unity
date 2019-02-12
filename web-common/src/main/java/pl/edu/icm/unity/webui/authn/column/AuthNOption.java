/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package pl.edu.icm.unity.webui.authn.column;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;

/**
 * 
 * @author P.Piernik
 *
 */
public class AuthNOption
{
	public final AuthenticationFlow flow;
	public final VaadinAuthentication authenticator;
	public final VaadinAuthenticationUI authenticatorUI;

	public AuthNOption(AuthenticationFlow flow, VaadinAuthentication authenticator,
			VaadinAuthenticationUI authenticatorUI)
	{
		this.flow = flow;
		this.authenticator = authenticator;
		this.authenticatorUI = authenticatorUI;
	}
}