/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Properties;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewProvider;

import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnNotifier;

/**
 * Implementations provide {@link View}s for registration in {@link SecuredNavigationUI}.
 * 
 * @author K. Benedyczak
 */
public interface SecuredViewProvider extends ViewProvider
{
	/**
	 * Provides well-known URL endpoint configuration to the view provider.
	 * @param configuration
	 */
	void setEndpointConfiguration(Properties configuration);

	/**
	 * Sets sandbox notifier. If the provider does not require this object can have nop implementation.
	 * @param sandboxNotifier
	 */
	void setSandboxNotifier(SandboxAuthnNotifier sandboxNotifier, String sandboxUrlForAssociation);
}
