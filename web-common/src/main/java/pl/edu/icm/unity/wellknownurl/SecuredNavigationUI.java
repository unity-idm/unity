/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.wellknownurl;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.server.utils.UnityMessageSource;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

/**
 * The Vaadin UI providing a concrete view depending on URL fragment. Actual views are configured via DI.
 * This variant is for use with protected resources, i.e. those requiring an authentication to gain access.
 * 
 * @author K. Benedyczak
 */
@Component("SecuredNavigationUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class SecuredNavigationUI extends GenericNavigationUI<SecuredViewProvider>
{
	@Autowired
	public SecuredNavigationUI(UnityMessageSource msg, Collection<SecuredViewProvider> viewProviders)
	{
		super(msg, viewProviders);
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		super.appInit(request);
		for (SecuredViewProvider viewProvider: viewProviders)
			viewProvider.setSandboxNotifier(sandboxRouter, getSandboxServletURLForAssociation());
	}
}


