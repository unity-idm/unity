/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.rest.doc;

import java.io.IOException;

import pl.edu.icm.unity.engine.server.UnityApplication;


public class TstServer
{
	public static void main(String... args) throws IOException 
	{
		UnityApplication theServer = new UnityApplication();
		theServer.run(new String[] {"src/main/resources/unityServer.conf"});
	}
}
