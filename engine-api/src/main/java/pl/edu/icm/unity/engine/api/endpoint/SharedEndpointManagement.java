/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.endpoint;

import org.eclipse.jetty.servlet.FilterHolder;
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
	public static final String CONTEXT_PATH = "/unitygw";
	String CONTEXT_PATH_2 = "/unitygw2";
	String POLICY_DOCUMENTS_PATH = "/pub/policyDocuments/";
	String REGISTRATION_PATH = "/pub/registration/";
	String ENQUIRY_PATH = "/pub/enquiry/";

	/**
	 * Deploys the given servlet in the internal, shared endpoint.
	 * @param contextPath path to the deployed servlet, will be the next element after the common context of 
	 * the whole internal endpoint.
	 * @param servlet the servlet to deploy
	 * @throws EngineException
	 */
	void deployInternalEndpointServlet(String contextPath, ServletHolder servlet, 
			boolean mapVaadinResource) throws EngineException;
		
	/**
	 * @return the first element of the servlet's path, with a leading '/' and no trailing '/'.
	 */
	String getBaseContextPath();

	/**
	 * 
	 * @param servletPath last path element of the servlet, without context prefix.
	 * @return URL in string form, including the server's address, shared context address and 
	 * the servlet's address. 
	 */
	String getServletUrl(String servletPath);

	/**
	 * Deploys the given filter in the internal, shared endpoint.
	 * @param contextPath
	 * @param filter
	 * @throws EngineException
	 */
	void deployInternalEndpointFilter(String contextPath, FilterHolder filter)
			throws EngineException;

	/**
	 * @return advertised address of the server
	 */
	String getServerAddress();
}
