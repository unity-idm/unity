/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Vaadin UI of the sandbox application. This UI is using the same authenticators as those configured for
 * the wrapping endpoint. Suitable for account association of not existing account with an existing one.
 *  
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("AccountAssociationSandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class AccountAssociationSandboxUI extends UnityUIBase implements UnityWebUI
{
	private LocaleChoiceComponent localeChoice;
	private InteractiveAuthenticationProcessor authnProcessor;
	private ExecutorsService execService;
	private EntityManagement idsMan;
	private List<AuthenticationFlow> authnFlows;
	private AuthenticationScreen ui;
	private ImageAccessService imageAccessService;
	
	@Autowired
	public AccountAssociationSandboxUI(MessageSource msg, ImageAccessService imageAccessService,
			LocaleChoiceComponent localeChoice,
			InteractiveAuthenticationProcessor authnProcessor,
			ExecutorsService execService, 
			@Qualifier("insecure") EntityManagement idsMan,
			AuthenticatorSupportService authenticatorSupport)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.imageAccessService = imageAccessService;
	}
	
	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationFlow> authnFlows,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		this.authnFlows = authnFlows;
		super.configure(description, authnFlows, registrationConfiguration, genericEndpointConfiguration);
	}
	
	//TODO KB - customize appInit/loadInitialState also with 2nd F support
	
	@Override
	protected void appInit(final VaadinRequest request)
	{
		String title = msg.getMessage("SandboxUI.authenticateToAssociateAccounts");
		ui = new SandboxAuthenticationScreen(msg, 
				imageAccessService,
				config, 
				endpointDescription, 
				cancelHandler, 
				idsMan, 
				execService, 
				authnProcessor, 
				localeChoice, 
				authnFlows,
				title,
				sandboxRouter,
				true);
		setContent(ui);
		setSizeFull();
	}
}
