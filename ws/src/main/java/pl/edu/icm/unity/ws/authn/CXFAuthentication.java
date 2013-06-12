/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.ws.authn;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link CXFEndpoint}.
 * @author K. Benedyczak
 */
public interface CXFAuthentication extends BindingAuthn
{
	public static final String NAME = "webservice-cxf2";
	
	/**
	 * @return interceptor to be installed. The interceptor should collect the authentication material 
	 * and store it in the context.
	 */
	public Interceptor<? extends Message> getInterceptor();
	
	/**
	 * Should return the result of the authentication of the authentication material of this 
	 * {@link CredentialRetrieval} stored in the context.
	 * The implementation need not to cache the result - it is guaranteed that endpoint will call
	 * this method only once per request.
	 * @return
	 */
	public AuthenticationResult getAuthenticationResult();
}
