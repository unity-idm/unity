/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out;

import java.util.Set;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;
import pl.edu.icm.unity.base.authn.AuthenticationMethod.Factor;

class AuthenticationMethodsToMvelContextMapper
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
		Set<AuthenticationMethod> recognizedMethods = usedMethods.stream().filter( a -> !a.factor.equals(Factor.UNKNOWN))
				.collect(Collectors.toSet());		
		if (recognizedMethods.size() > 1 && recognizedMethods.contains(AuthenticationMethod.SMS))
		{
			methods.add(MCA);
		}
		if (recognizedMethods.stream()
				.map(a -> a.factor)
				.collect(Collectors.toSet())
				.size() > 1)
		{
			methods.add(MFA);
		}

		return methods.stream().map(a -> a.toLowerCase()).collect(Collectors.toUnmodifiableSet());
	}

}
