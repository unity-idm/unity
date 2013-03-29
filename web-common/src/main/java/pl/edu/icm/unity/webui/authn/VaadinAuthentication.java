/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
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
	
	/**
	 * @return true if the retrieval requires username to be provided. Username is provided 
	 * from a component shared by all authenticators in a set. 
	 */
	public boolean needsCommonUsernameComponent();
	
	/**
	 * @return UI component associated with this retrieval
	 */
	public Component getComponent();
	
	/**
	 * Invoked only when {@link #needsCommonUsernameComponent()} returns true. 
	 * @param usernameCallback
	 */
	public void setUsernameCallback(UsernameProvider usernameCallback);
	
	/**
	 * Should trigger the actual authentication (if was not triggered manually via the component)
	 * and return the result of the authentication.
	 * @return
	 */
	public AuthenticationResult getAuthenticationResult();
	
	
	
	/**
	 * Can be used by retriever to get the username which is actually entered.
	 * @author K. Benedyczak
	 */
	public interface UsernameProvider
	{
		public String getUsername();
	}
}
