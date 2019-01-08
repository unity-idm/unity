/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.wellknownurl;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;

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
	private Properties configuration;
	
	@Autowired
	public SecuredNavigationUI(UnityMessageSource msg, Collection<SecuredViewProvider> viewProviders)
	{
		super(msg, viewProviders);
	}
	
	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationFlow> authenticationFlow,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		super.configure(description, authenticationFlow, registrationConfiguration, genericEndpointConfiguration);
		this.configuration = genericEndpointConfiguration;
	}
	
	@Override
	protected void appInit(VaadinRequest request)
	{
		super.appInit(request);
		for (SecuredViewProvider viewProvider: viewProviders)
		{
			viewProvider.setEndpointConfiguration(configuration);
			viewProvider.setSandboxNotifier(sandboxRouter, getSandboxServletURLForAssociation());
		}
	}
}


