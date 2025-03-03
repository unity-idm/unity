/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.Optional;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;

/**
 * Describes context of authentication: in which realm it happens (or happened), what flow is used, which option
 * was used and which factor it was which can be 1 or 2 (at least until we start support 3rd factor authn).
 */
public class AuthenticationStepContext extends AuthenticatorStepContext
{
	public final AuthenticationOptionKey authnOptionId;
	public final Optional<SigInInProgressContext> loginInProgressContext;
	
	public AuthenticationStepContext(AuthenticationRealm realm, AuthenticationFlow selectedAuthnFlow,
			AuthenticationOptionKey authnOptionId, FactorOrder factor, String endpointPath, Optional<SigInInProgressContext> loginInProgressContext)
	{
		super(realm, selectedAuthnFlow, endpointPath, factor);
		this.authnOptionId = authnOptionId;
		this.loginInProgressContext = loginInProgressContext;
	}

	public AuthenticationStepContext(AuthenticatorStepContext authenticatorContext,
			AuthenticationOptionKey authnOptionId, Optional<SigInInProgressContext> loginInProgressContext)
	{
		super(authenticatorContext);
		this.authnOptionId = authnOptionId;
		this.loginInProgressContext = loginInProgressContext;
	}

}
