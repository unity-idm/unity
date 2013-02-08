/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import pl.edu.icm.unity.server.UnityApplication;


public class TestServer
{
	@Test
	public void test() throws IOException 
	{
		FileUtils.deleteDirectory(new File("target/data"));
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
