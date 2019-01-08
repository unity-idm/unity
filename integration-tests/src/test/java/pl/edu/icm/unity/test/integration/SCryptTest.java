/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.integration;

import java.nio.charset.StandardCharsets;

import org.bouncycastle.crypto.generators.SCrypt;

import com.google.common.base.Stopwatch;

public class SCryptTest
{
	
	
	public static void main(String... args)
	{
		String password = "MargharetThacherIron";
		String salt = "1234567890123456789012345678901234567890123456789012345678901234";
		Stopwatch watch = Stopwatch.createStarted();
		byte[] enc = null;
		int N = 100;
		for (int i=0; i<N; i++)
		{
			enc = encode(password + i, salt + i);
			System.out.println("Res: " + (char)enc[0]);
		}
		System.out.println("After " + N + ": " + watch);
		
	}
	
	private static byte[] encode(String password, String salt)
	{
		return SCrypt.generate(password.getBytes(StandardCharsets.UTF_8), 
				salt.getBytes(StandardCharsets.UTF_8),
				1 << 15,	//cost 
				8, 		//block size
				1, 		//parallelization
				512/8);		//length
		//Memory use = 128 bytes × cost × blockSize
	}
}
