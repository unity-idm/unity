/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.api;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
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
	 * @param displayedName endpoint name to be used in UI.
	 * @param address
	 * @param configuration
	 * @throws EngineException 
	 */
	public EndpointDescription deploy(String typeId, String endpointName, I18nString displayedName, 
			String address, String description,
			List<AuthenticationOptionDescription> authn, String configuration, String realm) throws EngineException;


	/**
	 * Removes a deployed endpoint
	 * @param id endpoint instance id.
	 */
	public void undeploy(String id) throws EngineException;
	
	/**
	 * Updates a deployed endpoint configuration 
	 * @param id mandatory id of a deployed endpoint
	 * @param displayedName name of the endpoint which is presented to end users
	 * @param description new description, can be null to ignore the change
	 * @param authn new authentication configuration. Can be null to ignore.
	 * @param realm authentication realm to use
	 * @param configuration new json configuration, can be null to be unchanged.
	 */
	public void updateEndpoint(String id, I18nString displayedName, String description, 
			List<AuthenticationOptionDescription> authn, String configuration, String realm) throws EngineException;
}
