/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.types.endpoint.EndpointTypeDescription;

/**
 * Management of endpoints
 * @author K. Benedyczak
 */
public interface EndpointManagement
{
	/**
	 * @return available endpoint types
	 */
	public List<EndpointTypeDescription> getEndpointTypes();
	
	/**
	 * @return list of deployed endpoints
	 */
	public List<EndpointDescription> getEndpoints();
	
	/**
	 * Deploys a new instance of an endpoint of id type, at address location.
	 * Address is a path in web app context for the servlet endpoints.  
	 * @param id
	 * @param address
	 */
	public EndpointDescription deploy(String id, String address);

	/**
	 * Removes a deployed endpoint
	 * @param id endpoint instance id.
	 */
	public void undeploy(String id);
	
	/**
	 * Updates a deployed endpoint configuration 
	 * @param id mandatory id of a deployed endpoint
	 * @param description new description, can be null to ignore the change
	 * @param authn new authentication configuration. Can be null to ignore.
	 */
	public void updateEndpoint(String id, String description, List<AuthenticatorSet> authn);
}
