/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

import static io.imunity.otp.TOTPCodeGeneratorTest.hexToBase32;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.imunity.otp.TOTPCodeGeneratorTest.TestCaseSpec;

public class TOTPCodeVerificatorTest
{
	private static final TestCaseSpec CASE = new TestCaseSpec(1111111109, "07081804");

	@Test
	public void shouldVerifyInPastWithinOneStep()
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                boolean verification = TOTPCodeVerificator.verifyCode(CASE.code, seed, CASE.time * 1000 - 30001,
                		new OTPGenerationParams(8, HashFunction.SHA1, 30), 1);
                
                assertThat(verification).isTrue();
	}

	@Test
	public void shouldVerifyInFutureWithinOneStep()
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                boolean verification = TOTPCodeVerificator.verifyCode(CASE.code, seed, CASE.time * 1000 + 30001, 
                		new OTPGenerationParams(8, HashFunction.SHA1, 30), 1);
                
                assertThat(verification).isTrue();
	}

	@Test
	public void shouldVerifyPerfectlyInTimeWithoutDrift()
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                boolean verification = TOTPCodeVerificator.verifyCode(CASE.code, seed, CASE.time * 1000, 
                		new OTPGenerationParams(8, HashFunction.SHA1, 30), 0);
                
                assertThat(verification).isTrue();
	}

	@Test
	public void shouldVerifyPerfectlyInTimeWithDrift()
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                boolean verification = TOTPCodeVerificator.verifyCode(CASE.code, seed, CASE.time * 1000, 
                		new OTPGenerationParams(8, HashFunction.SHA1, 30), 1);
                
                assertThat(verification).isTrue();
	}

	@Test
	public void shouldNotVerifyInFutureOutsideAllowedDrift()
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                boolean verification = TOTPCodeVerificator.verifyCode(CASE.code, seed, CASE.time * 1000+60001, 
                		new OTPGenerationParams(8, HashFunction.SHA1, 30), 1);
                
                assertThat(verification).isFalse();
	}

	@Test
	public void shouldNotVerifyInPastOutsideAllowedDrift()
	{
		String seed = hexToBase32("3132333435363738393031323334353637383930");
		
                boolean verification = TOTPCodeVerificator.verifyCode(CASE.code, seed, CASE.time * 1000-60001, 
                		new OTPGenerationParams(8, HashFunction.SHA1, 30), 1);
                
                assertThat(verification).isFalse();
	}
}
