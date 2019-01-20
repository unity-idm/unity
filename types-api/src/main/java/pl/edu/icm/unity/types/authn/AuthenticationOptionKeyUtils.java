/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.types.authn;

/**
 * Creates and parses string representation containing authenticator key and its authN option key.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationOptionKeyUtils
{
	public static String encode(String authenticatorKey, String optionKey)
	{
		return optionKey != null ? authenticatorKey + "." + optionKey : authenticatorKey;
	}

	public static String encodeToCSS(String key)
	{
		return key.replace(".", "-");
	}
	
	public static String decodeOption(String globalKey)
	{
		int dotIndex = globalKey.indexOf(".");
		if (dotIndex == -1 || dotIndex == globalKey.length()-1)
			return null;
		return globalKey.substring(globalKey.indexOf(".")+1, globalKey.length());
	}
	
	public static String decodeAuthenticator(String globalKey)
	{
		int dotIndex = globalKey.indexOf(".");
		if (dotIndex == -1)
			return globalKey;
		return globalKey.substring(0, globalKey.indexOf("."));
	}
}
