/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.JsonSerializable;

/**
 * Retrieves credential, using a some binding specific method. Cooperates 
 * with {@link CredentialVerificator} via {@link CredentialExchange}.
 * <p>
 * Implementations must be thread safe and immutable.
 * @author K. Benedyczak
 */
public interface CredentialRetrieval extends BindingAuthn, JsonSerializable
{
	/**
	 * Sets initial state, given by the framework (the retrieval's own, implementation specific configuration
	 * is set via {@link #setSerializedConfiguration(String)}): the credential verificator and the configured id.
	 * @param e
	 * @param id
	 */
	public void setCredentialExchange(CredentialExchange e, String id);
	public String getBindingName();
}