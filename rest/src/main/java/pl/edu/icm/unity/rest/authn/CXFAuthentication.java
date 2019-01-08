/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.rest.authn;

import java.util.Properties;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Message;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the CXF based endpoints. Both REST and SOAP endpoints share the same contract, though names are
 * different (as well as the implementations may be quite different, due to other headers which are available
 * in the {@link Message} which is made available to the interceptor.
 * 
 * @author K. Benedyczak
 */
public interface CXFAuthentication extends BindingAuthn
{
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
	 * @param endpointFeatures properties with special features of the wrapping endpoint that may influence the authentication
	 * @return
	 */
	public AuthenticationResult getAuthenticationResult(Properties endpointFeatures);
}
