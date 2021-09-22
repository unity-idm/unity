/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp;

public enum HashFunction 
{
	SHA1("HmacSHA1", 160),
	SHA256("HmacSHA256", 256), 
	SHA512("HmacSHA512", 512);
	
	String alg;
	int bitLength;

	private HashFunction(String alg, int bitLength)
	{
		this.alg = alg;
		this.bitLength = bitLength;
	}
}