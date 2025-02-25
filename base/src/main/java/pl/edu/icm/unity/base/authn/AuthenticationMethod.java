/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.base.authn;

public enum AuthenticationMethod
{
	SMS(Factor.HAVE),
	SWK(Factor.HAVE),
	PWD(Factor.KNOW),
	HWK(Factor.HAVE),
	OTP(Factor.HAVE),
	U_OAUTH(Factor.UNKNOWN),
	U_SAML(Factor.UNKNOWN),
	U_LLC(Factor.UNKNOWN),
	UNKNOWN (Factor.UNKNOWN);

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
