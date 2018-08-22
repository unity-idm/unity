/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.Navigator.PushStateManager;
import com.vaadin.navigator.View;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.engine.api.wellknown.PublicWellKnownURLServletProvider;

/**
 * Custom navigation state manager based on {@link PushStateManager}.
 * 
 * The part of path after UI's "root path" (UI's path without view identifier)
 * is used as {@link View}s identifier. The rest of the path after the view name
 * can be used for extra parameters for the View.
 * 
 * The implementation does not support setting state, as the original
 * {@link PushStateManager} requires the server push to be present. Setting
 * state is not used by the
 * {@link PublicWellKnownURLServletProvider#SERVLET_PATH} so this functionality
 * was omitted.
 *
 * @author Roman Krysinski (roman@unity-idm.eu)
 */
class PathBasedNavigationStateManager extends PushStateManager
{
	public PathBasedNavigationStateManager(UI ui)
	{
		super(ui);
	}

	@Override
	public void setNavigator(Navigator navigator)
	{
		// nop
	}

	@Override
	public void setState(String state)
	{
		throw new IllegalStateException("operation not supported");
	}
}
