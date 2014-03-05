/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.test.integration;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import pl.edu.icm.unity.server.UnityApplication;


public class TstServer
{
	public static void main(String... args) throws IOException 
	{
		//FileUtils.deleteDirectory(new File("target/data"));
		UnityApplication.main(new String[] {"src/test/resources/unityServer.conf"});
	}
}
