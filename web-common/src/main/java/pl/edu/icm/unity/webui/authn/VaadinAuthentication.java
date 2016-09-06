/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Collection;
import java.util.Set;

import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.VaadinEndpoint;

import com.vaadin.server.Resource;
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
	Collection<VaadinAuthenticationUI> createUIInstance();
	
	
	public interface VaadinAuthenticationUI
	{
		/**
		 * @return UI component associated with this retrieval. If the returned component implements 
		 * the {@link Focusable} interface it will be focused after showing. 
		 * Important: this method must return the same instance of the {@link Component} for its lifetime. 
		 * The instance creation must be performed when the {@link VaadinAuthentication#createUIInstance()}
		 * is called.
		 */
		Component getComponent();
		
		/**
		 * Sets a callback object which is used to communicate the authentication result back to the 
		 * main authentication framework. 
		 * @param callback
		 */
		void setAuthenticationResultCallback(AuthenticationResultCallback callback);
	
		
		
		/**
		 * Sets a callback object which is used to indicate sandbox authentication. The result of 
		 * authn is returned back to the sandbox servlet. 
		 * @param callback
		 */
		void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback);
		
		/**
		 * Should trigger the actual authentication (if was not triggered manually via the component).
		 * If it is possible the implementation should invoke 
		 * {@link AuthenticationResultCallback#setAuthenticationResult(AuthenticationResult)}
		 * method inside of the implementation of this method. Some of the implementations may need to 
		 * initiate a long-running process with browser redirections after this method is called. Those must
		 * set the authentication result ASAP after it is available. 
		 */
		void triggerAuthentication();
		
		/**
		 * If called the authenticator should cancel the ongoing authentication if any. It can be called only
		 * after the {@link #triggerAuthentication()} was called and before the authenticator invoked callback.
		 */
		void cancelAuthentication();
		
		/**
		 * @return label for presentation in the user interface.
		 * returns non null value.
		 */
		String getLabel();
		
		/**
		 * @return set of optional tags which are attached to the authN option. 
		 * The tags are available to IdPs search feature
		 */
		Set<String> getTags();
		
		/**
		 * @return image {@link Resource} for the presentation in the user interface. Can be null.
		 */
		Resource getImage();
		
		/**
		 * Called after login was cancelled or finished, so the component can clear its state. 
		 */
		void clear();

		/**
		 * Invoked when browser refreshes.
		 * @param request that caused UI to be reloaded 
		 */
		void refresh(VaadinRequest request);

		/**
		 * @return unique identifier of this authentication option. The id must be unique among  
		 * ids returned by all {@link VaadinAuthenticationUI} of the {@link VaadinAuthentication}
		 */
		String getId();
		
		/**
		 * Used only if this authenticator is being used as a second authenticator during 2 way authentication.
		 * This method provides an entity which was authenticated by the primary authenticator. 
		 * The implementation may ignore this information, or use it to simplify the authentication 
		 * component. It is not needed to anyhow check if the provided entity with this method is equal to
		 * the one returned after authentication from this authenticator; this is verified by the framework.
		 * @param authenticatedEntity
		 */
		void presetEntity(Entity authenticatedEntity);
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
