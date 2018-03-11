/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.api.utils;

import java.security.SecureRandom;
import java.util.Random;

import com.google.common.primitives.Chars;

/**
 * Simply string code generator
 * @author P.Piernik
 *
 */
public class CodeGenerator
{
	private static final char[] LETTER_CHARS_POOL = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 
			'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
			'Z', 'X', 'C', 'V', 'B', 'N', 'M'};
	private static final char[] NUMBER_CHARS_POOL = { '1', '2', '3', '4', '5', '6', '7', '8',
			'9', '0' };
	private static final char[] MIXED_CHARS_POOL = Chars.concat(LETTER_CHARS_POOL, NUMBER_CHARS_POOL);
	
	private static final Random rnd = new SecureRandom();

	public static String generateMixedCharCode(int codeLenght)
	{
		return generateCode(codeLenght, MIXED_CHARS_POOL);
	}
	
	public static String generateNumberCode(int codeLenght)
	{
		return generateCode(codeLenght, NUMBER_CHARS_POOL);	
	}

	public static String generateCode(int codeLenght, char[] pool )
	{
		char[] codeA = new char[codeLenght];
		for (int i=0; i<codeLenght; i++)
			codeA[i] = pool[rnd.nextInt(pool.length)];
		return  new String(codeA);
	}
}
