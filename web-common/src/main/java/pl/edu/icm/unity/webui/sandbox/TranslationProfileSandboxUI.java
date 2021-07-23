/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.JavaScript;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportService;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.EndpointRegistrationConfiguration;
import pl.edu.icm.unity.webui.UnityUIBase;
import pl.edu.icm.unity.webui.UnityWebUI;
import pl.edu.icm.unity.webui.authn.AuthenticationScreen;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.remote.RemoteAuthnResponseProcessingFilter;
import pl.edu.icm.unity.webui.authn.remote.RemoteAuthnResponseProcessingFilter.PostAuthenticationDecissionWithContext;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Vaadin UI of the sandbox application using all remote authenticators. Suitable for sandbox authn used in 
 * input translation profile creation wizard and dry run.
 *  
 * @author Roman Krysinski
 */
@org.springframework.stereotype.Component("TranslationProfileSandboxUI")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Theme("unityThemeValo")
public class TranslationProfileSandboxUI extends UnityUIBase implements UnityWebUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, TranslationProfileSandboxUI.class);
	public static final String PROFILE_VALIDATION = "validate";
	
	private final AuthenticatorSupportService authenticatorSupport;
	private final LocaleChoiceComponent localeChoice;
	private final InteractiveAuthenticationProcessor authnProcessor;
	private final ExecutorsService execService;
	private final EntityManagement idsMan;
	private final ImageAccessService imageAccessService;

	private List<AuthenticationFlow> authnFlows;
	
	@Autowired
	public TranslationProfileSandboxUI(MessageSource msg, 
			LocaleChoiceComponent localeChoice,
			InteractiveAuthenticationProcessor authnProcessor,
			ExecutorsService execService, 
			@Qualifier("insecure") EntityManagement idsMan,
			AuthenticatorSupportService authenticatorSupport, ImageAccessService imageAccessService)
	{
		super(msg);
		this.localeChoice = localeChoice;
		this.authnProcessor = authnProcessor;
		this.execService = execService;
		this.idsMan = idsMan;
		this.authenticatorSupport = authenticatorSupport;
		this.imageAccessService = imageAccessService;
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
		loadInitialState();
	}

	
	private void loadInitialState() 
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		PostAuthenticationDecissionWithContext postAuthnStepDecision = (PostAuthenticationDecissionWithContext) session
				.getAttribute(RemoteAuthnResponseProcessingFilter.DECISION_SESSION_ATTRIBUTE);
		if (postAuthnStepDecision != null)
		{
			log.debug("Remote authentication result found in session, closing");
			JavaScript.getCurrent().execute("window.close();");
		} else
		{
			createAuthnUI();
		}
	}
	
	private void createAuthnUI()
	{
		boolean validationMode = isInProfileValidationMode();
		AuthenticationScreen ui = new SandboxAuthenticationScreen(msg,
				imageAccessService,
				config, 
				endpointDescription, 
				cancelHandler, 
				idsMan, 
				execService, 
				authnProcessor, 
				localeChoice, 
				authnFlows,
				getTitle(validationMode),
				sandboxRouter,
				false);
		setContent(ui);
		setSizeFull();
	}

	private boolean isInProfileValidationMode()
	{
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		return vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
	}

	private String getTitle(boolean validationMode)
	{
		return validationMode ? msg.getMessage("SandboxUI.selectionTitle.profileValidation") : 
			msg.getMessage("SandboxUI.selectionTitle.profileCreation");
	}
	
	private List<AuthenticationFlow> getAllRemoteVaadinAuthenticators() 
	{
		try
		{
			return authenticatorSupport.getRemoteAuthenticatorsAsFlows(VaadinAuthentication.NAME);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}
	}
}
