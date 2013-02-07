/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.integration;

import org.junit.Test;

import pl.edu.icm.unity.server.UnityApplication;


public class TestServer
{
	@Test
	public void test()
	{
		UnityApplication theServer = new UnityApplication();
		theServer.run(new String[] {"src/test/resources/unityServer.conf"});
		try
		{
			synchronized(this)
			{
				wait();
			}
		} catch (InterruptedException e)
		{
		}
	}
}
