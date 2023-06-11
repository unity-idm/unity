/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

import org.springframework.context.ApplicationContext;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.webui.authn.CancelHandler;

/**
 * Creates UI object by retrieving it from Spring context.
 *  
 * @author K. Benedyczak
 */
public class VaadinUIProvider extends UIProvider
{
	private transient ApplicationContext applicationContext;
	private transient String uiBeanName;
	private transient ResolvedEndpoint description;
	private transient Supplier<List<AuthenticationFlow>> authenticationFlows;
	private transient CancelHandler cancelHandler;
	private transient SandboxAuthnRouter sandboxRouter;
	private transient EndpointRegistrationConfiguration registrationConfiguraiton;
	private transient Properties endpointProperties;
	private transient String themeConfigKey;

	public VaadinUIProvider(ApplicationContext applicationContext, String uiBeanName,
			ResolvedEndpoint description, Supplier<List<AuthenticationFlow>> authenticationFlows,
			EndpointRegistrationConfiguration registrationConfiguraiton,
			Properties properties, String themeConfigKey)
	{
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.description = description;
		this.authenticationFlows = authenticationFlows;
		this.registrationConfiguraiton = registrationConfiguraiton;
		this.endpointProperties = properties;
		this.themeConfigKey = themeConfigKey;
	}


	public void setCancelHandler(CancelHandler cancelHandler)
	{
		this.cancelHandler = cancelHandler;
	}

	public void setSandboxRouter(SandboxAuthnRouter sandboxRouter) 
	{
		this.sandboxRouter = sandboxRouter;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Class<? extends UI> getUIClass(UIClassSelectionEvent event)
	{
		return (Class<? extends UI>) applicationContext.getType(uiBeanName);
	}

	
	/**
	 * Sets theme to the one defined with the given key. If not set then the default theme from the configuration
	 * is set. If this is also undefined nothing is changed (in practice: fall back to theme defined with annotation).
	 * @param properties
	 * @param mainKey
	 */
	@Override
	public String getTheme(UICreateEvent event) 
	{
		VaadinEndpointProperties properties = new VaadinEndpointProperties(endpointProperties);
		String configuredTheme = properties.getConfiguredTheme(themeConfigKey);
		return configuredTheme == null ? super.getTheme(event) : configuredTheme;
	}
	
	@Override
	public UI createInstance(UICreateEvent event)
	{
		UI ui = (UI) applicationContext.getBean(uiBeanName);
		if (ui instanceof UnityWebUI)
		{
			if (sandboxRouter != null) 
				((UnityWebUI)ui).setSandboxRouter(sandboxRouter);
			((UnityWebUI)ui).configure(description, authenticationFlows.get(), registrationConfiguraiton,
					endpointProperties);
			if (cancelHandler != null)
			{
				((UnityWebUI)ui).setCancelHandler(cancelHandler);
			}
		}
		return ui;
	}
}
