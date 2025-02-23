/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.translation.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import pl.edu.icm.unity.base.authn.AuthenticationMethod;

public class AuthenticationMethodsMapperTest
{

	@Test
	public void shouldAddMFA()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.pwd, AuthenticationMethod.hwk))).containsExactlyInAnyOrder("hwk", "pwd",
						"mfa");

	}

	@Test
	public void shouldAddMCA()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.pwd, AuthenticationMethod.sms))).containsExactlyInAnyOrder("sms", "pwd",
						"mfa", "mca");

	}
	
	@Test
	public void shouldNotAddMCAIfUnknownMethod()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.u_oauth, AuthenticationMethod.sms))).containsExactlyInAnyOrder("sms", "u_oauth");

	}

	@Test
	public void shouldNotAddMFA()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.otp, AuthenticationMethod.hwk))).containsExactlyInAnyOrder("hwk", "otp");

	}
	
	@Test
	public void shouldNotAddMFAIfUnknownMethod()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.otp, AuthenticationMethod.u_saml))).containsExactlyInAnyOrder("u_saml", "otp");

	}
}
