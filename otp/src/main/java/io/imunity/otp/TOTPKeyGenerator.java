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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Optional;

import org.apache.commons.codec.binary.Base32;
import org.apache.hc.core5.net.URIBuilder;

public class TOTPKeyGenerator
{
	public static final String SECRET_URI_PARAM = "secret";
	public static final String ISSUER_URI_PARAM = "issuer";
	public static final String ALGORITHM_URI_PARAM = "algorithm";
	public static final String DIGITS_URI_PARAM = "digits";
	public static final String PERIOD_URI_PARAM = "period";
	public static final String IMAGE_URI_PARAM = "image";
	
	private static final SecureRandom RNG = new SecureRandom();
	private static final Base32 BASE32_ENCODER = new Base32();
	
	public static String generateTOTPURI(String secretBase32, String label, String issuer, OTPGenerationParams otpParams, Optional<String> logoUri)
	{
		if (issuer.contains(":"))
			throw new IllegalArgumentException("Issuer can not contain colon");
		if (label.contains(":"))
			throw new IllegalArgumentException("Label can not contain colon");
		try
		{
			URIBuilder uriBuilder = new URIBuilder("otpauth://totp/" 
					+ URLEncoder.encode(issuer, StandardCharsets.UTF_8).replace("+", "%20") + ":" 
					+ URLEncoder.encode(label, StandardCharsets.UTF_8).replace("+", "%20"));
			uriBuilder.addParameter(SECRET_URI_PARAM, secretBase32); 
			uriBuilder.addParameter(ISSUER_URI_PARAM, issuer); 
			uriBuilder.addParameter(ALGORITHM_URI_PARAM, otpParams.hashFunction.toString()); 
			uriBuilder.addParameter(DIGITS_URI_PARAM, String.valueOf(otpParams.codeLength)); 
			uriBuilder.addParameter(PERIOD_URI_PARAM, String.valueOf(otpParams.timeStepSeconds)); 
			logoUri.ifPresent(uri -> uriBuilder.addParameter(IMAGE_URI_PARAM, uri));
			return uriBuilder.build().toASCIIString();
		} catch (URISyntaxException e)
		{
			throw new IllegalArgumentException("Can not encode OTP URI", e);
		}
	}
	
	static String generateRandomBase32EncodedKey(HashFunction hashFunction)
	{
		int bytes = hashFunction.bitLength / 8;
		byte[] key = new byte[bytes];
		RNG.nextBytes(key);
		return BASE32_ENCODER.encodeAsString(key);
	}
}
