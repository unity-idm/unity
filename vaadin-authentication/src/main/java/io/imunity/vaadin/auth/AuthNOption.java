/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.vaadin.auth;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;

public class AuthNOption
{
	public final AuthenticationFlow flow;
	public final VaadinAuthentication authenticator;
	public final VaadinAuthentication.VaadinAuthenticationUI authenticatorUI;

	public AuthNOption(AuthenticationFlow flow, VaadinAuthentication authenticator,
	                   VaadinAuthentication.VaadinAuthenticationUI authenticatorUI)
	{
		this.flow = flow;
		this.authenticator = authenticator;
		this.authenticatorUI = authenticatorUI;
	}
}