/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.server;

import java.net.URL;

import org.eclipse.jetty.servlet.ServletContextHandler;

import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to the information of the network server.
 * @author K. Benedyczak
 */
public interface NetworkServer
{
	/**
	 * @return base address of the server which should be used as its externally accessible address.
	 */
	URL getAdvertisedAddress();
	
	void deployEndpoint(WebAppEndpointInstance endpoint) 
			throws EngineException;
	
	void undeployEndpoint(String id) throws EngineException;

	void deployHandler(ServletContextHandler sharedHandler) throws EngineException;

	void undeployAllHandlers() throws EngineException;
}
