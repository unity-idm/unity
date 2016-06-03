/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.utils;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.digests.SHA256Digest;

/**
 * Shared crypto code.
 * @author K. Benedyczak
 */
public class CryptoUtils
{
	/**
	 * @param password string to be hashed
	 * @param salt salt to be concatenated
	 * @return SHA254 hash of the salted argument.
	 */
	public static byte[] hash(String password, String salt, int rehashNumber)
	{
		byte[] interim = (salt+password).getBytes(StandardCharsets.UTF_8);
		SHA256Digest digest = new SHA256Digest();
		int size = digest.getDigestSize();
		
		for (int i=0; i<rehashNumber; i++)
			interim = hash(interim, size, digest);
		
		return interim;
	}

	private static byte[] hash(byte[] current, int size, SHA256Digest digest)
	{
		digest.update(current, 0, current.length);
		byte[] hashed = new byte[size];
		digest.doFinal(hashed, 0);
		return hashed;
	}
}
