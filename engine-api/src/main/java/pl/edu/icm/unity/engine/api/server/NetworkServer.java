/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.server;

import java.util.Set;

import org.eclipse.jetty.servlet.ServletContextHandler;

import pl.edu.icm.unity.engine.api.endpoint.WebAppEndpointInstance;
import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Provides access to the information of the network server.
 * @author K. Benedyczak
 */
public interface NetworkServer
{
	void deployEndpoint(WebAppEndpointInstance endpoint) 
			throws EngineException;
	
	void undeployEndpoint(String id) throws EngineException;

	void deployHandler(ServletContextHandler sharedHandler) throws EngineException;

	void undeployHandler(String contextPath) throws EngineException;
	
	void undeployAllHandlers() throws EngineException;
	
	Set<String> getUsedContextPaths();
}
