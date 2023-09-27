/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.authn;

import jakarta.servlet.http.HttpSession;

public enum AuthenticationPolicyEE10
{

	DEFAULT, FORCE_LOGIN, REQUIRE_EXISTING_SESSION;

	private static final String AUTHENTICATION_POLICY_ATTRIBUTE = "authnPolicy";

	public static void setPolicy(HttpSession session, AuthenticationPolicyEE10 policy)
	{
		session.setAttribute(AUTHENTICATION_POLICY_ATTRIBUTE, policy);
	}

	public static AuthenticationPolicyEE10 getPolicy(HttpSession session)
	{
		Object attribute = session.getAttribute(AUTHENTICATION_POLICY_ATTRIBUTE);
		if (attribute != null)
		{
			return (AuthenticationPolicyEE10) attribute;
		}

		return AuthenticationPolicyEE10.DEFAULT;
	}
}