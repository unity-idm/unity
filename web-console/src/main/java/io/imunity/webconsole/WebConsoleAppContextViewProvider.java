/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */


package io.imunity.webconsole;

import com.vaadin.navigator.View;

import io.imunity.webelements.navigation.AppContextViewProvider;
import io.imunity.webelements.navigation.NavigationHierarchyManager;
import pl.edu.icm.unity.webui.sandbox.SandboxAuthnRouter;

/**
 * Works as {@link AppContextViewProvider} but also set {@link SandboxAuthnRouter} in {@link UnityViewWithSandbox}
 * @author P.Piernik
 *
 */
public class WebConsoleAppContextViewProvider extends AppContextViewProvider
{

	private SandboxAuthnRouter sandboxRouter;
	
	public WebConsoleAppContextViewProvider(NavigationHierarchyManager navMan, SandboxAuthnRouter sandboxRouter)
	{
		super(navMan);
		this.sandboxRouter = sandboxRouter;
	}

	@Override
	public View getView(String viewName)
	{	
		View view = super.getView(viewName);
		if (view == null)
			return null;
		
		if (view instanceof UnityViewWithSandbox)
		{
			((UnityViewWithSandbox) view).setSandboxRouter(sandboxRouter);
		}
		return view;
	}
}
