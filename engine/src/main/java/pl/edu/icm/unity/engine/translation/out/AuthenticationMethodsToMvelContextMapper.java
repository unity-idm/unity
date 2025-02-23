/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out;

import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationMethod.Factor;

public class AuthenticationMethodsToMvelContextMapper
{
	private static final String MFA = "mfa";
	private static final String MCA = "mca";

	public static Set<String> getAuthenticationMethodsWithMFAandMCAIfUsed(Set<AuthenticationMethod> usedMethods)
	{
		if (usedMethods == null)
			return Set.of();
		
		Set<String> methods = usedMethods.stream()
				.map(a -> a.name())
				.collect(Collectors.toSet());

		if (usedMethods.size() > 1 && usedMethods.contains(AuthenticationMethod.sms))
		{
			methods.add(MCA);
		}

		if (usedMethods.stream().filter( a -> !a.factor.equals(Factor.UNKNOWN))
				.map(a -> a.factor)
				.collect(Collectors.toSet())
				.size() > 1)
		{
			methods.add(MFA);
		}

		return methods;

	}

}
