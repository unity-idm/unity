/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.authn;

import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.engine.api.utils.StringConfigurable;

/**
 * Retrieves credential, using a some binding specific method. Cooperates 
 * with {@link CredentialVerificator} via {@link CredentialExchange}.
 * <p>
 * Implementations must be thread safe and immutable.
 * @author K. Benedyczak
 */
public interface CredentialRetrieval extends BindingAuthn, StringConfigurable
{
	/**
	 * Sets initial state, given by the framework (the retrieval's own, implementation specific configuration
	 * is set via {@link #setSerializedConfiguration(com.fasterxml.jackson.databind.node.ObjectNode)}): 
	 * the credential verificator and the configured id.
	 * @param e
	 * @param id
	 */
	void setCredentialExchange(CredentialExchange e, String id);
	String getBindingName();
	
	default boolean requiresRedirect()
	{
		return false;
	}
}