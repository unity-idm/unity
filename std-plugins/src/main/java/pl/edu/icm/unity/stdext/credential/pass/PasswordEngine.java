/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.Arrays;


/**
 * Low level password handling.
 * Allows for initial obfuscation of a given password (PasswordInfo is generated, 
 * ready to be stored in DB) and for checking a given password against the one loaded.
 */
public class PasswordEngine
{
	private static final int SALT_LENGTH = 32;
	private Random random = new SecureRandom();
	private final SCryptEncoder scryptEncoder;
	
	public PasswordEngine(ForkJoinPool pool)
	{
		this.scryptEncoder = new SCryptEncoder(pool);
	}
	
	PasswordInfo prepareForStore(PasswordCredential credentialSettings, String password)
	{
		byte[] salt = genSalt();
		ScryptParams scryptParams = credentialSettings.getScryptParams();
		byte[] hash = scryptEncoder.scrypt(password, salt, scryptParams);
		return new PasswordInfo(PasswordHashMethod.SCRYPT, 
				hash, 
				salt, 
				scryptParams.toMap(), 
				System.currentTimeMillis());
	}

	boolean verify(PasswordInfo stored, String password)
	{
		PasswordHashMethod method = stored.getMethod();
		switch (method)
		{
		case SCRYPT:
			return verifySCrypt(stored, password);
		case SHA256:
			return verifySHA2(stored, password);
		}
		throw new IllegalStateException("Shouldn't happen: "
				+ "unsupported password hash method: " + method);
	}
	
	private boolean verifySHA2(PasswordInfo stored, String password)
	{
		Map<String, Object> methodParams = stored.getMethodParams();
		int rehashNumber = (Integer)methodParams.getOrDefault("rehashNumber", 1);
		String salt = stored.getSalt() == null ? "" :
			new String(stored.getSalt(), StandardCharsets.UTF_8);
		byte[] interim = (salt+password).getBytes(StandardCharsets.UTF_8);
		SHA256Digest digest = new SHA256Digest();
		int size = digest.getDigestSize();
		
		for (int i=0; i<rehashNumber; i++)
			interim = sha2hash(interim, size, digest);
		
		return Arrays.areEqual(interim, stored.getHash());
	}

	private boolean verifySCrypt(PasswordInfo stored, String password)
	{
		ScryptParams params = new ScryptParams(stored.getMethodParams());
		byte[] testedHash = scryptEncoder.scrypt(password, stored.getSalt(), params);
		return Arrays.areEqual(testedHash, stored.getHash());
	}

	boolean checkParamsUpToDate(PasswordCredential credentialSettings, PasswordInfo stored)
	{
		if (stored.getMethod() != PasswordHashMethod.SCRYPT)
			return credentialSettings.isAllowLegacy();

		ScryptParams params = new ScryptParams(stored.getMethodParams());
		return credentialSettings.getScryptParams().equals(params);
	}

	private byte[] genSalt()
	{
		byte[] salt = new byte[SALT_LENGTH];
		random.nextBytes(salt);
		return salt;
	}
	
	private byte[] sha2hash(byte[] current, int size, SHA256Digest digest)
	{
		digest.update(current, 0, current.length);
		byte[] hashed = new byte[size];
		digest.doFinal(hashed, 0);
		return hashed;
	}
}
