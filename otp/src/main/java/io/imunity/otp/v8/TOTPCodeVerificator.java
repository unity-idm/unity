/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.v8;

import io.imunity.otp.OTPGenerationParams;

public class TOTPCodeVerificator
{
	public static boolean verifyCode(String code, String secret, OTPGenerationParams params, int allowedDriftSteps)
	{
		long currentTime = System.currentTimeMillis();
		return verifyCode(code, secret, currentTime, params, allowedDriftSteps);
	}
	
	public static boolean verifyCode(String code, String secret, long currentTime, OTPGenerationParams params, int allowedDriftSteps)
	{
		long currentTimeStep = currentTime/1000;
		for (int i=0; i<=allowedDriftSteps; i++)
			if (verifyCodeWithoutDrift(code, secret, params, currentTimeStep + i * params.timeStepSeconds))
				return true;
		for (int i=-allowedDriftSteps; i<0; i++)
			if (verifyCodeWithoutDrift(code, secret, params, currentTimeStep + i * params.timeStepSeconds))
				return true;
		return false;
	}

	private static boolean verifyCodeWithoutDrift(String code, String secret, OTPGenerationParams params, long atTime)
	{
		String correctCode = TOTPCodeGenerator.generateTOTP(secret, atTime, params);
		return correctCode.equals(code);
	}
}
