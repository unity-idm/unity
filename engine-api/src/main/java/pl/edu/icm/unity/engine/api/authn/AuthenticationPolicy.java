/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import javax.servlet.http.HttpSession;

public enum AuthenticationPolicy
{

	DEFAULT, FORCE_LOGIN, REQUIRE_EXISTING_SESSION;

	private static final String AUTHENTICATION_POLICY_ATTRIBUTE = "authnPolicy";

	public static void setPolicy(HttpSession session, AuthenticationPolicy policy)
	{
		session.setAttribute(AUTHENTICATION_POLICY_ATTRIBUTE, policy);
	}

	public static AuthenticationPolicy getPolicy(HttpSession session)
	{
		Object attribute = session.getAttribute(AUTHENTICATION_POLICY_ATTRIBUTE);
		if (attribute != null)
		{
			return (AuthenticationPolicy) attribute;
		}

		return AuthenticationPolicy.DEFAULT;
	}
}