/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

/**
 * Creates and parses string representation containing authenticator key and its authN option key.
 * 
 * @author K. Benedyczak
 */
public class AuthenticationOptionKeyUtils
{
	public static String encode(String authenticationFlowKey, String authenticatorKey, String optionKey)
	{
		return authenticationFlowKey + "." + authenticatorKey + "." + optionKey;
	}
	
	public static String decodeAuthenticationFlow(String globalKey)
	{
		return globalKey.substring(0, globalKey.indexOf("."));
	}

	public static String decodeAuthenticator(String globalKey)
	{
		return globalKey.substring(globalKey.indexOf(".") + 1, globalKey.indexOf(".", 1));
	}

	public static String decodeOption(String globalKey)
	{
		return globalKey.substring(globalKey.indexOf(".", 1) + 1, globalKey.length());
	}
}
