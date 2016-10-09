/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static org.junit.Assert.assertNotEquals;

import java.net.URL;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.exceptions.EngineException;

@RunWith(SpringJUnit4ClassRunner.class)
@UnityIntegrationTest
public class AllInterfacesBindTest
{
	@Autowired
	protected JettyServer httpServer;
	
	@After
	public void clear() throws EngineException
	{
		httpServer.stop();
	}
	
	@Test
	public void testAllInterfaces()
	{
		httpServer.start();
		URL url = httpServer.getAdvertisedAddress();
		assertNotEquals("0.0.0.0", url.getHost());
		assertNotEquals("0", url.getPort());
	}
	
}
