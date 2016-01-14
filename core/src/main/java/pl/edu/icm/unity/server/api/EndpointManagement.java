/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
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
	public List<EndpointTypeDescription> getEndpointTypes() throws EngineException;
	
	/**
	 * @return list of deployed endpoints
	 */
	public List<EndpointDescription> getEndpoints() throws EngineException;
	
	/**
	 * Deploys a new instance of an endpoint of id type, at address location.
	 * Address is a path in web app context for the servlet endpoints.  
	 * @param typeId
	 * @param endpointName identifier to be given to the endpoint
	 * @param address
	 * @param configuration
	 * @throws EngineException 
	 */
	public EndpointDescription deploy(String typeId, String endpointName,  
			String address, EndpointConfiguration configuration) throws EngineException;


	/**
	 * Removes a deployed endpoint
	 * @param id endpoint instance id.
	 */
	public void undeploy(String id) throws EngineException;
	
	/**
	 * Updates a deployed endpoint configuration 
	 * @param id mandatory id of a deployed endpoint
	 * @param configuration updated configuration, can have null elements to leave the existing values unchanged.
	 */
	public void updateEndpoint(String id, EndpointConfiguration configuration) throws EngineException;
}
