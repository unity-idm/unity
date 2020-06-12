/*
 * Original work:
 * 
 * Copyright (c) 2011 IETF Trust and the persons identified as
 * authors of the code. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted pursuant to, and subject to the license
 * terms contained in, the Simplified BSD License set forth in Section
 * 4.c of the IETF Trust's Legal Provisions Relating to IETF Documents
 * (http://trustee.ietf.org/license-info).
 * 
 * Adaptation to Unity:
 * 
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

public class TOTPCodeVerificator
{
	public static boolean verifyCode(String code, String secret, OTPGenerationParams params, int allowedDriftSteps)
	{
		long currentTime = System.currentTimeMillis()/1000;
		for (int i=0; i<=allowedDriftSteps; i++)
			if (verifyCodeWithoutDrift(code, secret, params, currentTime + i * params.timeStepSeconds))
				return true;
		for (int i=-allowedDriftSteps; i<0; i++)
			if (verifyCodeWithoutDrift(code, secret, params, currentTime + i * params.timeStepSeconds))
				return true;
		return false;
	}
	
	private static boolean verifyCodeWithoutDrift(String code, String secret, OTPGenerationParams params, long atTime)
	{
		String correctCode = TOTPCodeGenerator.generateTOTP(secret, atTime, params);
		return correctCode.equals(code);
	}
}
