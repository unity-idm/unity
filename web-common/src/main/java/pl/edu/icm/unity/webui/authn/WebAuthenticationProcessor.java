/*
 * Copyright (c) 2018 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Optional;

import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;

public interface WebAuthenticationProcessor
{

	/**
	 * Return partial authn result if additional authentication is required or null, if authentication is 
	 * finished. In the latter case a proper redirection is triggered. If outdated credential was used
	 * then the credential update dialog is shown instead of the redirection to origin. 
	 * @param result
	 * @param clientIp
	 * @param realm
	 * @param authenticationOption
	 * @param rememberMe
	 * @return
	 * @throws AuthenticationException
	 */
	Optional<PartialAuthnState> processPrimaryAuthnResult(AuthenticationResult result, String clientIp,
			AuthenticationRealm realm, AuthenticationFlow authenticationFlow, boolean rememberMe, String firstFactorAuthnOptionId)
			throws AuthenticationException;

	void processSecondaryAuthnResult(PartialAuthnState state, AuthenticationResult result2, String clientIp,
			AuthenticationRealm realm, AuthenticationFlow authenticationFlow, boolean rememberMe, String secondFactorAuthnOptionId)
			throws AuthenticationException;

	void logout();

	void logout(boolean soft);

}