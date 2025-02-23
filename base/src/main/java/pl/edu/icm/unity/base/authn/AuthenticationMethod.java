/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.authn;

public enum AuthenticationMethod
{
	sms(Factor.HAVE), swk(Factor.HAVE), pwd(Factor.KNOW), hwk(Factor.HAVE), otp(Factor.HAVE), u_oauth(Factor.UNKNOWN),
	u_saml(Factor.UNKNOWN) , u_llc(Factor.UNKNOWN), unkwown (Factor.UNKNOWN);

	public final Factor factor;

	AuthenticationMethod(Factor factor)
	{
		this.factor = factor;
	}

	public enum Factor
	{
		HAVE, KNOW, UNKNOWN
	};
}
