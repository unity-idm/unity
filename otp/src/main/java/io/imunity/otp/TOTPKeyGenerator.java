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

import java.net.URISyntaxException;
import java.security.SecureRandom;

import org.apache.commons.codec.binary.Base32;
import org.apache.http.client.utils.URIBuilder;

public class TOTPKeyGenerator
{
	private static final SecureRandom RNG = new SecureRandom();
	private static final Base32 BASE32_ENCODER = new Base32();
	
	public static String generateTOTPURI(String secretBase32, String label, String issuer, OTPGenerationParams otpParams)
	{
		if (issuer.contains(":"))
			throw new IllegalArgumentException("Issuer can not contain colon");
		if (label.contains(":"))
			throw new IllegalArgumentException("Label can not contain colon");
		try
		{
			URIBuilder uriBuilder = new URIBuilder("otpauth://totp/");
			uriBuilder.setPath(issuer + ":" + label);
			uriBuilder.addParameter("secret", secretBase32); 
			uriBuilder.addParameter("issuer", issuer); 
			uriBuilder.addParameter("algorithm", otpParams.hashFunction.toString()); 
			uriBuilder.addParameter("digits", String.valueOf(otpParams.codeLength)); 
			uriBuilder.addParameter("period", String.valueOf(otpParams.timeStepSeconds)); 
			return uriBuilder.build().toASCIIString();
		} catch (URISyntaxException e)
		{
			throw new IllegalArgumentException("Can not encode OTP URI", e);
		}
	}
	
	public static String generateRandomBase32EncodedKey(HashFunction hashFunction)
	{
		int bytes = hashFunction.bitLength / 8;
		byte[] key = new byte[bytes];
		RNG.nextBytes(key);
		return BASE32_ENCODER.encodeAsString(key);
	}
}
