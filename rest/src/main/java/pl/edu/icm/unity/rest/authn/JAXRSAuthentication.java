/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import javax.ws.rs.container.ContainerRequestFilter;

import pl.edu.icm.unity.rest.RESTEndpoint;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link RESTEndpoint}.
 * 
 * @author K. Benedyczak
 */
public interface JAXRSAuthentication extends BindingAuthn
{
	public static final String NAME = "jaxrs2";
	
	/**
	 * @return filter to be installed. The filter should collect the authentication material 
	 * and store it in the context.
	 */
	public ContainerRequestFilter getFilter();
	
	/**
	 * Should return the result of the authentication of the authentication material of this 
	 * {@link CredentialRetrieval} stored in the context.
	 * The implementation need not to cache the result - it is guaranteed that endpoint will call
	 * this method only once per request.
	 * @return
	 */
	public AuthenticationResult getAuthenticationResult();
}
