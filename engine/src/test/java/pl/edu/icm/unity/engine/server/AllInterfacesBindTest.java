/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;


import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.UnityIntegrationTest;
import pl.edu.icm.unity.engine.api.server.AdvertisedAddressProvider;

@ExtendWith(SpringExtension.class)
@UnityIntegrationTest
public class AllInterfacesBindTest
{
	@Autowired
	protected JettyServer httpServer;
	@Autowired
	protected AdvertisedAddressProvider advertisedAddrProvider;
	
	@AfterEach
	public void clear() throws EngineException
	{
		httpServer.stop();
	}
	
	@Test
	public void testAllInterfaces()
	{
		httpServer.start();
		URL url = advertisedAddrProvider.get();
		assertNotEquals("0.0.0.0", url.getHost());
		assertNotEquals("0", url.getPort());
	}
	
}
