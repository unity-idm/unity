/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.types.authn.AuthenticationRealm;

/**
 * Describes authenticator context of authentication: in which realm it happens (or happened), what flow is used,
 * which factor it was which can be 1 or 2 (at least until we start support 3rd factor authn).
 */
public class AuthenticatorStepContext
{
	public final AuthenticationRealm realm; 
	public final AuthenticationFlow selectedAuthnFlow; 
	public final int factor; 
	
	public AuthenticatorStepContext(AuthenticationRealm realm, AuthenticationFlow selectedAuthnFlow, int factor)
	{
		this.realm = realm;
		this.selectedAuthnFlow = selectedAuthnFlow;
		this.factor = factor;
	}
	
	public AuthenticatorStepContext(AuthenticatorStepContext toClone)
	{
		this(toClone.realm, toClone.selectedAuthnFlow, toClone.factor);
	}
}
