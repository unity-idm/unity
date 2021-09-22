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

import java.security.GeneralSecurityException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base32;
import org.bouncycastle.util.encoders.Hex;

public class TOTPCodeGenerator
{
	private static final int[] DIGITS_POWER = {1, 10, 100, 1000, 10000, 100000, 1000000, 10000000, 100000000};
	private static final Base32 BASE32_ENCODER = new Base32();
	
	public static String generateTOTP(String keyBase32, long timestampSec, OTPGenerationParams params)
	{
		String time = getTimeStepHexEncoded(timestampSec, params.timeStepSeconds);
		byte[] msg = Hex.decode(time);
		byte[] k = BASE32_ENCODER.decode(keyBase32);
		byte[] hash = hmacSHA(params.hashFunction.alg, k, msg);

		int offset = hash[hash.length - 1] & 0xf;

		int binary = ((hash[offset] & 0x7f) << 24) | ((hash[offset + 1] & 0xff) << 16)
				| ((hash[offset + 2] & 0xff) << 8) | (hash[offset + 3] & 0xff);

		int otp = binary % DIGITS_POWER[params.codeLength];

		String result = Integer.toString(otp);
		while (result.length() < params.codeLength)
		{
			result = "0" + result;
		}
		return result;
	}

	private static String getTimeStepHexEncoded(long time, int stepSeconds)
	{
		long T = time/stepSeconds;
                String steps = Long.toHexString(T).toUpperCase();
                while (steps.length() < 16) 
                	steps = "0" + steps;
                return steps;
	}
	
	private static byte[] hmacSHA(String crypto, byte[] keyBytes, byte[] text)
	{
		try
		{
			Mac hmac = Mac.getInstance(crypto);
			SecretKeySpec macKey = new SecretKeySpec(keyBytes, "RAW");
			hmac.init(macKey);
			return hmac.doFinal(text);
		} catch (GeneralSecurityException e)
		{
			throw new IllegalStateException("Can't create hash MAC", e);
		}
	}
}
