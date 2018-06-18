/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.google.common.collect.Sets;
import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;

import pl.edu.icm.unity.engine.api.AuthenticatorManagement;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorsRegistry;
import pl.edu.icm.unity.engine.api.authn.CredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition.Policy;
import pl.edu.icm.unity.types.authn.AuthenticatorInstance;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Vaadin UI of the sandbox application using all remote authenticators. Suitable for sandbox authn used in 
 * input translation profile creation wizard and dry run.
 *  
 * @author Roman Krysinski
 */
@org.springframework.stereotype.Component("TranslationProfileSandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
@PreserveOnRefresh
public class TranslationProfileSandboxUI extends UnityUIBase implements UnityWebUI
{
	public static final String PROFILE_VALIDATION = "validate";
	
	private final AuthenticatorsRegistry authnRegistry;
	private final AuthenticatorManagement authenticationManagement;
	private final AuthenticatorSupportManagement authenticatorSupport;

	
	private LocaleChoiceComponent localeChoice;
	private SandboxAuthenticationProcessor authnProcessor;
	private ExecutorsService execService;
	private EntityManagement idsMan;
	private List<AuthenticationFlow> authnFlows;
	private AuthenticationScreen ui;
	
	@Autowired
	public TranslationProfileSandboxUI(UnityMessageSource msg, 
			LocaleChoiceComponent localeChoice,
			SandboxAuthenticationProcessor authnProcessor,
			ExecutorsService execService, 
			@Qualifier("insecure") EntityManagement idsMan,
			AuthenticatorsRegistry authnRegistry,
			AuthenticatorManagement authenticationManagement,
			AuthenticatorSupportManagement authenticatorSupport)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.authnRegistry = authnRegistry;
		this.authenticationManagement = authenticationManagement;
		this.authenticatorSupport = authenticatorSupport;
	}
	
	@Override
	public void configure(ResolvedEndpoint description,
			List<AuthenticationFlow> authnFlows,
			EndpointRegistrationConfiguration registrationConfiguration,
			Properties genericEndpointConfiguration)
	{
		this.authnFlows = getAllRemoteVaadinAuthenticators();
		super.configure(description, this.authnFlows, registrationConfiguration, genericEndpointConfiguration);
	}
	
	@Override
	protected void appInit(final VaadinRequest request)
	{
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		boolean validationMode = vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
		
		ui = new SandboxAuthenticationScreen(msg, 
				config, 
				endpointDescription, 
				cancelHandler, 
				idsMan, 
				execService, 
				authnProcessor, 
				localeChoice, 
				sandboxRouter,
				authnFlows,
				getTitle(validationMode));
		setContent(ui);
		setSizeFull();
	}
	

	private String getTitle(boolean validationMode)
	{
		return validationMode ? msg.getMessage("SandboxUI.selectionTitle.profileValidation") : 
			msg.getMessage("SandboxUI.selectionTitle.profileCreation");
	}
	
	@Override
	protected void refresh(VaadinRequest request) 
	{
		ui.refresh(request);
	}
	
	
	private List<AuthenticationFlow> getAllRemoteVaadinAuthenticators() 
	{
		ArrayList<AuthenticationFlowDefinition> flows = new ArrayList<>();
		
		try 
		{
			Collection<AuthenticatorInstance> authnInstances = authenticationManagement.getAuthenticators(
					VaadinAuthentication.NAME);
			for (AuthenticatorInstance instance : authnInstances)
			{
				CredentialVerificatorFactory factory = authnRegistry.getCredentialVerificatorFactory(
						instance.getTypeDescription().getVerificationMethod());
				if (!(factory instanceof LocalCredentialVerificatorFactory)) 
				{
					AuthenticationFlowDefinition authnFlow = new AuthenticationFlowDefinition(
							instance.getId(), Policy.NEVER, Sets.newHashSet(instance.getId()));
					flows.add(authnFlow);
				}
			}
		} catch (EngineException e) 
		{
			throw new IllegalStateException("Unable to initialize sandbox servlet: failed to get authenticators: " 
					+ e.getMessage(), e);
		}
		return createFlowInstances(flows);
	}
	
	private List<AuthenticationFlow> createFlowInstances(List<AuthenticationFlowDefinition> authnList) 
	{
		try
		{
			return authenticatorSupport.getAuthenticatorUIs(authnList);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}
	}
}
