/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.integration;

import java.io.IOException;

import pl.edu.icm.unity.server.UnityApplication;


public class TstServer
{
	public static void main(String... args) throws IOException 
	{
		UnityApplication.main(new String[] {"src/test/resources/unityServer.conf"});
	}
}
