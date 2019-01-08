/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.CredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.endpoint.BindingAuthn;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.VaadinEndpoint;

/**
 * Defines a contract which must be implemented by {@link CredentialRetrieval}s in order to be used 
 * with the {@link VaadinEndpoint}.
 * @author K. Benedyczak
 */
public interface VaadinAuthentication extends BindingAuthn
{
	public static final String NAME = "web-vaadin7";
	
	public enum Context
	{
		LOGIN,
		REGISTRATION
	}
	
	/**
	 * @return a new instance of the credential retrieval UIs. The collection is returned as one authenticator 
	 * may provide many authN options (e.g. many remote IdPs). 
	 */
	Collection<VaadinAuthenticationUI> createUIInstance(Context context);
	
	/**
	 * @return true only if {@link VaadinAuthenticationUI#getGridCompatibleComponent()} 
	 * is allowed for this implementation.
	 */
	boolean supportsGrid();
	
	
	public interface VaadinAuthenticationUI
	{
		/**
		 * @return UI component associated with this retrieval.
		 * Important: this method must return the same instance of the {@link Component} for its lifetime. 
		 * The instance creation must be performed when the {@link VaadinAuthentication#createUIInstance()}
		 * is called.
		 */
		Component getComponent();

		/**
		 * @return simplified UI widget that is suitable for inclusion in a gird. If this implementation is
		 * not supporting such variant should throw an exception.  
		 */
		default Component getGridCompatibleComponent()
		{
			throw new UnsupportedOperationException("This authenticator is not grid compatible");
		}
		
		/**
		 * Sets a callback object which is used to communicate the authentication result back to the 
		 * main authentication framework. 
		 */
		void setAuthenticationCallback(AuthenticationCallback callback);
	
		/**
		 * Sets object allowing for triggering credential reset if the component supports this.
		 */
		default void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
		}
		
		/**
		 * TODO do we need separate ? 
		 * Sets a callback object which is used to indicate sandbox authentication. The result of 
		 * authn is returned back to the sandbox servlet. 
		 */
		void setSandboxAuthnCallback(SandboxAuthnResultCallback callback);
		
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
		
		
		
		default void disableCredentialReset()
		{
		}
		
		/**
		 * @return implementation may decide to disable this option if some runtime
		 * conditions are rendering it unusable.
		 */
		default boolean isAvailable()
		{
			return true;
		}
		
		/**
		 * System may use this method to set expected identity that should be returned by authentication
		 * facility. This is useful in case of remote authentication, in case when authentication 
		 * triggering subsystem expects a particular identity to sign in. 
		 */
		default void setExpectedIdentity(ExpectedIdentity expectedIdentity)
		{
		}
	}

	enum AuthenticationStyle 
	{
		IMMEDIATE,
		WITH_EMBEDDED_CANCEL,
		WITH_EXTERNAL_CANCEL
	}
	
	/**
	 * Retrieval must provide an authentication result via this callback ASAP, after it is triggered.
	 * @author K. Benedyczak
	 */
	public interface AuthenticationCallback
	{
		/**
		 * Should be always called after authentication is started
		 */
		void onStartedAuthentication(AuthenticationStyle authenticationStyle);
		
		/**
		 * Should be called after authentication result is obtained
		 */
		void onCompletedAuthentication(AuthenticationResult result);

		/**
		 * Should be called after authentication result is obtained and authentication has failed
		 */
		void onFailedAuthentication(AuthenticationResult result, String error, Optional<String> errorDetail);
		
		/**
		 * Should be called to signal the framework that authentication was cancelled/failed/stopped etc 
		 * in the component
		 */
		void onCancelledAuthentication();
	}
}
