/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api;

import java.util.List;

import pl.edu.icm.unity.base.endpoint.Endpoint;
import pl.edu.icm.unity.base.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.base.endpoint.EndpointTypeDescription;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.engine.api.authn.AuthorizationException;
import pl.edu.icm.unity.engine.api.endpoint.EndpointInstance;

/**
 * Management of endpoints
 * @author K. Benedyczak
 */
public interface EndpointManagement
{
	/**
	 * @return available endpoint types
	 */
	List<EndpointTypeDescription> getEndpointTypes() throws EngineException;
	
	/**
	 * @return list of deployed endpoints
	 */
	List<ResolvedEndpoint> getDeployedEndpoints() throws EngineException;
	
	/**
	 * Deploys a new instance of an endpoint of id type, at address location.
	 * Address is a path in web app context for the servlet endpoints.  
	 * @param typeId
	 * @param endpointName identifier to be given to the endpoint
	 * @param address
	 * @param configuration
	 * @throws EngineException 
	 */
	ResolvedEndpoint deploy(String typeId, String endpointName,  
			String address, EndpointConfiguration configuration) throws EngineException;


	/**
	 * Removes a deployed endpoint
	 * @param id endpoint instance id.
	 */
	void undeploy(String id) throws EngineException;
	
	/**
	 * Updates a deployed endpoint configuration 
	 * @param id mandatory id of a deployed endpoint
	 * @param configuration updated configuration, can have null elements to leave the existing values unchanged.
	 */
	void updateEndpoint(String id, EndpointConfiguration configuration) throws EngineException;

	/**
	 * 
	 * @return all endpoints from db
	 * @throws AuthorizationException 
	 */
	List<Endpoint> getEndpoints() throws AuthorizationException;
	
	
	/**
	 * Removes endpoint with given name
	 * @param id mandatory id of removed endpoint
	 * @throws EngineException
	 */
	void removeEndpoint(String id) throws EngineException;
	
	/**
	 * 
	 * @param name endpoint name
	 * @return 
	 * @throws AuthorizationException
	 */
	Endpoint getEndpoint(String name) throws AuthorizationException;

	/**
	 * @return list of deployed endpoints
	 */
	List<EndpointInstance> getDeployedEndpointInstances() throws AuthorizationException;
}
