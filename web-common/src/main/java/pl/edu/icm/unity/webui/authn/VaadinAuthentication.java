/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Collection;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.webui.VaadinEndpoint;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link VaadinEndpoint}.
 * @author K. Benedyczak
 */
public interface VaadinAuthentication extends BindingAuthn
{
	public static final String NAME = "web-vaadin7";
	
	/**
	 * @return a new instance of the credential retrieval UIs. The collection is returned as one authenticator 
	 * may provide many authN options (e.g. many remote IdPs). 
	 */
	public Collection<VaadinAuthenticationUI> createUIInstance();
	
	
	public interface VaadinAuthenticationUI
	{
		/**
		 * @return UI component associated with this retrieval. If the returned component implements 
		 * the {@link Focusable} interface it will be focused after showing
		 */
		public Component getComponent();
		
		/**
		 * Sets a callback object which is used to communicate the authentication result back to the 
		 * main authentication framework. 
		 * @param callback
		 */
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback);
		
		/**
		 * Sets a callback object which is used to indicate sandbox authentication. The result of 
		 * authn is returned back to the sandbox servlet. 
		 * @param callback
		 */
		public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback);
		
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
		 * @return image URL for the presentation in the user interface. Can be null.
		 */
		public String getImageURL();
		
		/**
		 * Called after login was cancelled or finished, so the component can clear its state. 
		 */
		public void clear();

		/**
		 * Invoked when browser refreshes.
		 * @param request that caused UI to be reloaded 
		 */
		public void refresh(VaadinRequest request);

		/**
		 * @return unique identifier of this authentication option. The id must be unique among  
		 * ids returned by all {@link VaadinAuthenticationUI} of the {@link VaadinAuthentication}
		 */
		public String getId();
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
