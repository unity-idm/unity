/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

public class TestEndpoints extends DBIntegrationTestBase
{
	@Test
	public void testEndpoints() throws Exception
	{
		List<EndpointTypeDescription> endpointTypes = endpointMan.getEndpointTypes();
		System.out.println(endpointTypes);
		assertEquals(1, endpointTypes.size());
		EndpointTypeDescription type = endpointTypes.get(0);
		
		endpointMan.deploy(type.getName(), "endpoint1", "/foo", "");
		List<EndpointDescription> endpoints = endpointMan.getEndpoints();
		assertEquals(1, endpoints.size());

		endpointMan.updateEndpoint(endpoints.get(0).getId(), "ada", null, null);
		endpoints = endpointMan.getEndpoints();
		assertEquals("ada", endpoints.get(0).getDescription());

		endpointMan.undeploy(endpoints.get(0).getId());
		endpoints = endpointMan.getEndpoints();
		assertEquals(0, endpoints.size());

		
		//test initial loading from DB: create, remove from the server, load
		
		endpointMan.deploy(type.getName(), "endpoint1", "/foo", "");
		endpointMan.deploy(type.getName(), "endpoint2", "/foo2", "");
		endpoints = endpointMan.getEndpoints();
		assertEquals(2, endpoints.size());
		endpointMan.updateEndpoint(endpoints.get(0).getId(), "endp1", null, null);
		endpointMan.updateEndpoint(endpoints.get(1).getId(), "endp2", null, null);

		httpServer.undeployEndpoint(endpoints.get(0).getId());
		httpServer.undeployEndpoint(endpoints.get(1).getId());

		endpoints = endpointMan.getEndpoints();
		assertEquals(0, endpoints.size());
		
		internalEndpointMan.loadPersistedEndpoints();
		
		endpoints = endpointMan.getEndpoints();
		assertEquals(2, endpoints.size());
		assertEquals("endp1", endpoints.get(0).getDescription());
		assertEquals("endp2", endpoints.get(1).getDescription());
		
		//finally test if removal from DB works
		
		httpServer.undeployEndpoint(endpoints.get(0).getId());
		httpServer.undeployEndpoint(endpoints.get(1).getId());

		internalEndpointMan.removeAllPersistedEndpoints();
		internalEndpointMan.loadPersistedEndpoints();
		endpoints = endpointMan.getEndpoints();
		assertEquals(0, endpoints.size());
	}
}
