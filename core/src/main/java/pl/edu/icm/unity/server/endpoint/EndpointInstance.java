/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import java.net.URL;
import java.util.List;
import java.util.Map;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.JsonSerializable;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.AuthenticatorSet;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Generic endpoint instance. Implementations must persist/load only the custom settings using the {@link JsonSerializable}
 * interface; authenticators, id, context address and description are always set via initialize method. 
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
	 * @param authenticatorsInfo generic info about authenticators set with their ids and groupings
	 * @param authenticators actual authenticators. the list has entries corresponding to the first argument.
	 * the map holds mappings of each authenticator name to its implementation
	 */
	public void initialize(String id, I18nString displayedName, URL baseAddress, String contextAddress, 
			String description, 
			List<AuthenticatorSet> authenticatorsInfo, List<Map<String, BindingAuthn>> authenticators,
			AuthenticationRealm realm, String serializedConfiguration);

	public EndpointDescription getEndpointDescription();
	
	/**
	 * @return the current list of previously configured authenticators (with initialize).
	 */
	public List<Map<String, BindingAuthn>> getAuthenticators();
	
	/**
	 * @return serialized representation of the endpoint configuration/state
	 */
	public String getSerializedConfiguration();

	public void destroy();
	
	
	/**
	 * Runtime update of the authenticators being used by this endpoint.
	 * @param handler
	 * @param authenticators
	 * @throws UnsupportedOperationException if the operation is unsupported and the endpoint must be 
	 * re-created instead.
	 */
	public void updateAuthenticators(List<Map<String, BindingAuthn>> authenticators)
		throws UnsupportedOperationException;
}
