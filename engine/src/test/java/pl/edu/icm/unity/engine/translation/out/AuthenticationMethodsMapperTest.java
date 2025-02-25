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
	public void shouldAddMFAWhenTwoFactorsAreUsed()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.PWD, AuthenticationMethod.HWK))).containsExactlyInAnyOrder("hwk", "pwd",
						"mfa");

	}

	@Test
	public void shouldAddMCAWhenSMSWithAnotherFactorAreUsed()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.PWD, AuthenticationMethod.SMS))).containsExactlyInAnyOrder("sms", "pwd",
						"mfa", "mca");

	}
	
	@Test
	public void shouldNotAddMCAIfUnknownMethod()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.U_OAUTH, AuthenticationMethod.SMS))).containsExactlyInAnyOrder("sms", "u_oauth");

	}

	@Test
	public void shouldNotAddMFAWhenTheSameFactor()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.OTP, AuthenticationMethod.HWK))).containsExactlyInAnyOrder("hwk", "otp");

	}
	
	@Test
	public void shouldNotAddMFAIfUnknownMethod()
	{
		assertThat(AuthenticationMethodsToMvelContextMapper.getAuthenticationMethodsWithMFAandMCAIfUsed(
				Set.of(AuthenticationMethod.OTP, AuthenticationMethod.U_SAML))).containsExactlyInAnyOrder("u_saml", "otp");

	}
}
