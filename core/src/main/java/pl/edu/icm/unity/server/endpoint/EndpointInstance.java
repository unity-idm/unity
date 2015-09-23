/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.util.List;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Generic endpoint instance. Implementations must persist/load only the custom settings using the {@link JsonSerializable}
 * interface; authenticators, id, and description are always set via initialize method. 
 * 
 * Lifecycle:
 * <ol>
 *  <li>initialize (once)
 *  
 *  <li>operation.... 
 *  
 *  <li>destroy (once)
 * </ol>
 * Destroy might be called also before initialize if there was server deployment error.
 * 
 * @author K. Benedyczak
 */
public interface EndpointInstance
{
	/**
	 * @param endpointDescription most of the endpoint's settings
	 * @param authenticatonOptions authenticator instances for the endpoint.
	 * @param serializedConfiguration endpoint specific configuration 
	 * (as returned by {@link #getSerializedConfiguration()}.
	 */
	void initialize(EndpointDescription endpointDescription, 
			List<AuthenticationOption> authenticatonOptions,
			String serializedConfiguration);

	EndpointDescription getEndpointDescription();
	
	/**
	 * @return the current list of previously configured authenticators (with initialize).
	 */
	List<AuthenticationOption> getAuthenticationOptions();
	
	/**
	 * @return serialized representation of the endpoint configuration/state
	 */
	String getSerializedConfiguration();

	/**
	 * Starts the endpoint. After this method returns the endpoint should be made available for usage.
	 * @throws EngineException 
	 */
	void start() throws EngineException;
	
	/**
	 * Stops the endpoint.
	 * @throws EngineException 
	 */
	void destroy() throws EngineException;
	
	
	/**
	 * Runtime update of the authenticators being used by this endpoint.
	 * @param handler
	 * @param authenticationOptions
	 * @throws UnsupportedOperationException if the operation is unsupported and the endpoint must be 
	 * re-created instead.
	 */
	void updateAuthenticationOptions(List<AuthenticationOption> authenticationOptions)
		throws UnsupportedOperationException;
}
