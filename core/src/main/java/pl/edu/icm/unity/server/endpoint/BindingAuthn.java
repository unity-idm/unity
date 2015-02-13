/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.endpoint;

import pl.edu.icm.unity.server.authn.CredentialRetrieval;

/**
 * Interface defining binding specific API (e.g. for Vaadin Web interface or CXF WS)
 * which must be implemented to plug an authentication to the binding. Nearly marker - the real
 * functionality will be available in extensions.
 * <p>
 * This interface is separate from the {@link CredentialRetrieval} to provide an endpoint-only view on a 
 * credential retrieval (what is needed by the endpoint to get authN results from an authenticator). 
 * The {@link CredentialRetrieval} is an extension with methods used by the engine to initialize the 
 * instance. The implementations will implement a binding specific interface (extension of this interface 
 * and the {@link CredentialRetrieval}.
 * <p>
 * IMPORTANT: The implementation MUST be thread safe by being immutable. Perfectly - stateless. 
 * This is because a single instance of the retrieval of the implemented type will be created per
 * retrieval configuration defined in the system. And this retrieval can be used in many places.
 * If binding-specific contract requires stateful credential retrieval (e.g. by providing a UI)
 * this must be achieved by using factory method(s) in the binding-specific retrieval interface.
 *   
 * @author K. Benedyczak
 */
public interface BindingAuthn
{
	/**
	 * @return implementation id
	 */
	public String getBindingName();
	
	/**
	 * @return name of the configured authenticator instance.
	 */
	public String getAuthenticatorId();
}
