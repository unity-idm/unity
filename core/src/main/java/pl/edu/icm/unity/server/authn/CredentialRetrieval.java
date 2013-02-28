/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import pl.edu.icm.unity.server.endpoint.BindingAuthn;

/**
 * Retrieves credential from a particular binding, using a particular method. Cooperates 
 * with verificator via implemented {@link CredentialExchange}.
 * @author K. Benedyczak
 */
public interface CredentialRetrieval extends CredentialExchange, BindingAuthn
{

}
