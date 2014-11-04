/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.as;

import static pl.edu.icm.unity.oauth.as.OAuthASProperties.*;

import java.util.Properties;

import javax.ws.rs.core.Response;

import org.junit.Test;

import pl.edu.icm.unity.oauth.as.token.DiscoveryResource;
import pl.edu.icm.unity.server.api.PKIManagement;

public class DiscoveryResourceTest
{
	@Test
	public void testDiscovery()
	{
		Properties properties = new Properties();
		properties.setProperty(P + ISSUER_URI, "https://localhost:233/token");
		properties.setProperty(P + CREDENTIAL, "MAIN");
		PKIManagement pkiManagement = new MockPKIMan();
		OAuthASProperties config = new OAuthASProperties(properties, pkiManagement, 
				"https://localhost:233/foo");
		OAuthEndpointsCoordinator coordinator = new OAuthEndpointsCoordinator();
		coordinator.registerAuthzEndpoint("https://localhost:233/token", "https://localhost:233/as");
		DiscoveryResource tested = new DiscoveryResource(config, coordinator);
		
		Response resp = tested.getMetadata();
		String body = resp.readEntity(String.class);
		System.out.println(body);
	}
}
