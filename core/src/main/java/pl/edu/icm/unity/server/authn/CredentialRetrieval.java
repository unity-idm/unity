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
 * Implementations must be thread safe.
 * @author K. Benedyczak
 */
public interface CredentialRetrieval extends BindingAuthn, JsonSerializable
{
	public void setCredentialExchange(CredentialExchange e);
}