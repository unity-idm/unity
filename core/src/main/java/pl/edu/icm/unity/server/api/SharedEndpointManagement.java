/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import org.eclipse.jetty.servlet.ServletHolder;

import pl.edu.icm.unity.exceptions.EngineException;

/**
 * Management of the single, shared, internal Unity endpoint,  which is not under administrator's control.
 * It is intended for a cross-cutting functionality, where Unity has to listen for some requests but 
 * on an path which is not endpoint specific (e.g. for SAML responses, where return address must be the same 
 * for all authenticators).
 * 
 * @author K. Benedyczak
 */
public interface SharedEndpointManagement
{
	
	/**
	 * Deploys the given servlet in the internal, shared endpoint.
	 * @param contextPath path to the deployed servlet, will be the next element after the common context of 
	 * the whole internal endpoint.
	 * @param servlet the servlet to deploy
	 * @throws EngineException
	 */
	public void deployInternalEndpointServlet(String contextPath, ServletHolder servlet) throws EngineException;
	
	/**
	 * @return the first element of the servlet's path, with a leading '/' and no trailing '/'.
	 */
	public String getBaseContextPath();
}
