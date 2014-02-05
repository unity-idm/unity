/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import com.vaadin.server.Resource;
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
	 * @return a new instance of the credential retrieval UI
	 */
	public VaadinAuthenticationUI createUIInstance();
	
	
	public interface VaadinAuthenticationUI
	{
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
		 * Sets a callback object which is used to communicate the authentication result back to the 
		 * main authentication framework. 
		 * @param callback
		 */
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback);
		
		/**
		 * Should trigger the actual authentication (if was not triggered manually via the component).
		 * If it is possible the implementation should invoke 
		 * {@link AuthenticationResultCallback#setAuthenticationResult(AuthenticationResult)}
		 * method inside of the implementation of this method. Some of the implementations may need to 
		 * initiate a long-running process with browser redirections after this method is called. Those must
		 * set the authentication result ASAP after it is available. 
		 */
		public void triggerAuthentication();
		
		/**
		 * If called the authenticator should cancel the ongoing authentication if any. It can be called only
		 * after the {@link #triggerAuthentication()} was called and before the authenticator invoked callback.
		 */
		public void cancelAuthentication();
		
		/**
		 * @return label for presentation in the user interface.
		 * returns non null value.
		 */
		public String getLabel();
		
		/**
		 * @return image for the presentation in the user interface. Can be null.
		 */
		public Resource getImage();
		
		/**
		 * Called after login was cancelled or finished, so the component can clear its state. 
		 */
		public void clear();
	}
		
	/**
	 * Can be used by retriever to get the username which is actually entered.
	 * @author K. Benedyczak
	 */
	public interface UsernameProvider
	{
		public String getUsername();
	}
	
	/**
	 * Retrieval must provide an authentication result via this callback ASAP, after it is triggered.
	 * @author K. Benedyczak
	 */
	public interface AuthenticationResultCallback
	{
		public void setAuthenticationResult(AuthenticationResult result);
		
		/**
		 * Should be called to signal the framework that authentication was cancelled/failed/stopped etc 
		 * in the component, so waiting for its finish makes no sense.
		 */
		public void cancelAuthentication();
	}

}
