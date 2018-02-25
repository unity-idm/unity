/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Simply string code generator
 * @author P.Piernik
 *
 */
public class CodeGenerator
{
	private static final char[] CHARS_POOL = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 
			'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
			'Z', 'X', 'C', 'V', 'B', 'N', 'M', 
			'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};

	private static final Random rnd = new SecureRandom();

	public static String generateCode(int codeLenght)
	{
		char[] codeA = new char[codeLenght];
		for (int i=0; i<codeLenght; i++)
			codeA[i] = CHARS_POOL[rnd.nextInt(CHARS_POOL.length)];
		return  new String(codeA);	
	}

}
