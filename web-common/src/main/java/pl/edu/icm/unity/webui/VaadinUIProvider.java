/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui;

import java.util.List;
import java.util.Properties;

import org.springframework.context.ApplicationContext;

import pl.edu.icm.unity.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.server.authn.AuthenticationOption;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;
import pl.edu.icm.unity.webui.authn.CancelHandler;

import com.vaadin.server.UIClassSelectionEvent;
import com.vaadin.server.UICreateEvent;
import com.vaadin.server.UIProvider;
import com.vaadin.ui.UI;

/**
 * Creates UI object by retrieving it from Spring context.
 *  
 * @author K. Benedyczak
 */
public class VaadinUIProvider extends UIProvider
{
	private static final long serialVersionUID = 1L;
	private transient ApplicationContext applicationContext;
	private transient String uiBeanName;
	private transient EndpointDescription description;
	private transient List<AuthenticationOption> authenticators;
	private transient CancelHandler cancelHandler;
	private transient SandboxAuthnRouter sandboxRouter;
	private transient EndpointRegistrationConfiguration registrationConfiguraiton;
	private transient Properties endpointProperties;

	public VaadinUIProvider(ApplicationContext applicationContext, String uiBeanName,
			EndpointDescription description, List<AuthenticationOption> authenticators,
			EndpointRegistrationConfiguration registrationConfiguraiton,
			Properties properties)
	{
		super();
		this.applicationContext = applicationContext;
		this.uiBeanName = uiBeanName;
		this.description = description;
		this.authenticators = authenticators;
		this.registrationConfiguraiton = registrationConfiguraiton;
		this.endpointProperties = properties;
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

	@Override
	public UI createInstance(UICreateEvent event)
	{
		UI ui = (UI) applicationContext.getBean(uiBeanName);
		if (ui instanceof UnityWebUI)
		{
			((UnityWebUI)ui).configure(description, authenticators, registrationConfiguraiton,
					endpointProperties);
			if (cancelHandler != null)
			{
				((UnityWebUI)ui).setCancelHandler(cancelHandler);
			}
			if (sandboxRouter != null) 
			{
				((UnityWebUI)ui).setSandboxRouter(sandboxRouter);
			}
		}
		return ui;
	}
}
