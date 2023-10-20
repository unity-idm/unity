/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import java.util.Date;
import java.util.Optional;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;

public interface RememberMeProcessorEE8
{
	public static final String REMEMBER_ME_TOKEN_TYPE = "rememberMe";
	
	Optional<LoginSession> processRememberedWholeAuthn(HttpServletRequest httpRequest, ServletResponse response,
			String clientIp, AuthenticationRealm realm, UnsuccessfulAuthenticationCounter dosGauard);

	Optional<LoginSession> processRememberedSecondFactor(HttpServletRequest httpRequest, ServletResponse response,
			long entityId, String clientIp, AuthenticationRealm realm,
			UnsuccessfulAuthenticationCounter dosGauard);

	void addRememberMeCookieAndUnityToken(HttpServletResponse response, AuthenticationRealm realm,
			LoginMachineDetails machineDetails, long entityId, Date loginTime,
			AuthenticationOptionKey firstFactorOptionId, AuthenticationOptionKey secondFactorOptionId);

	void removeRememberMeWithWholeAuthn(String realmName, HttpServletRequest request,
			HttpServletResponse httpResponse);

}