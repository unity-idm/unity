/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.webui.VaadinEndpoint;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link VaadinEndpoint}.
 * @author K. Benedyczak
 */
public interface VaadinAuthentication extends BindingAuthn
{
	public static final String NAME = "web-vaadin7";
}
