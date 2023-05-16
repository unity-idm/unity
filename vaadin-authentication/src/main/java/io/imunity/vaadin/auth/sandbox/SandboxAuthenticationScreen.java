/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.sandbox;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import io.imunity.vaadin.auth.*;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.LocaleChoiceComponent;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessor;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.types.authn.AuthenticationRealm;
import pl.edu.icm.unity.types.authn.RememberMePolicy;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.CancelHandler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.*;

/**
 * Extends authentication screen, reconfiguring it to work as sandbox one.
 */
public class SandboxAuthenticationScreen extends ColumnInstantAuthenticationScreen
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthenticationScreen.class);
	private final SandboxAuthnRouter sandboxRouter;

	public SandboxAuthenticationScreen(MessageSource msg,
	                                   VaadinLogoImageLoader imageAccessService,
	                                   VaadinEndpointProperties config,
	                                   ResolvedEndpoint endpointDescription,
	                                   CancelHandler cancelHandler,
	                                   EntityManagement idsMan,
	                                   ExecutorsService execService,
	                                   InteractiveAuthenticationProcessor authnProcessor,
	                                   Optional<LocaleChoiceComponent> localeChoice,
	                                   List<AuthenticationFlow> authenticators,
	                                   String title,
	                                   SandboxAuthnRouter sandboxRouter,
									   NotificationPresenter notificationPresenter,
	                                   boolean baseOnOriginalEndpointConfig)
	{
		super(msg, imageAccessService, baseOnOriginalEndpointConfig ? 
				prepareConfigurationBasingOnEndpoint(config.getProperties(), title) : 
				prepareFreshConfigurationWithAllOptions(title, authenticators), 
				endpointDescription, 
				new NoOpCredentialRestLauncher(),
				() -> {},
				cancelHandler, idsMan, 
				execService, false, 
				SandboxAuthenticationScreen::disabledUnknownUserProvider, 
				localeChoice, 
				authenticators,
				authnProcessor,
				notificationPresenter);
		this.sandboxRouter = sandboxRouter;
		init();
		checkNotNull(sandboxRouter);
	}

	private static VaadinEndpointProperties prepareFreshConfigurationWithAllOptions(String title,
			List<AuthenticationFlow> authenticators)
	{
		Properties sandboxConfig = new Properties();
		sandboxConfig.setProperty(PREFIX + AUTHN_TITLE, title);
		sandboxConfig.setProperty(PREFIX + AUTHN_SHOW_LAST_OPTION_ONLY, "false");
		sandboxConfig.setProperty(PREFIX + AUTHN_ADD_ALL, "true");
		
		String gridAuthnsSpec = getGridFlowsSpec(authenticators);
		String nonGridAuthnsSpec = getNonGridFlowsSpec(authenticators);
		
		sandboxConfig.setProperty(PREFIX + AUTHN_GRIDS_PFX + "G1." + AUTHN_GRID_CONTENTS, gridAuthnsSpec);
		sandboxConfig.setProperty(PREFIX + AUTHN_GRIDS_PFX + "G1." + AUTHN_GRID_ROWS, "15");
		sandboxConfig.setProperty(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "_GRID_G1 " + nonGridAuthnsSpec);
		sandboxConfig.setProperty(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_WIDTH, "28");
		
		log.debug("Configuration for the sandbox screen with all options:\n{}", sandboxConfig);
		return new VaadinEndpointProperties(sandboxConfig);
	}

	private static String getGridFlowsSpec(List<AuthenticationFlow> authenticators)
	{
		return authenticators.stream()
			.filter(flow -> flow.getFirstFactorAuthenticators().stream()
					.anyMatch(authenticator -> ((VaadinAuthentication)authenticator.getRetrieval()).supportsGrid()))
			.map(AuthenticationFlow::getId)
			.collect(Collectors.joining(" "));
	}

	private static String getNonGridFlowsSpec(List<AuthenticationFlow> authenticators)
	{
		return authenticators.stream()
			.flatMap(flow -> flow.getFirstFactorAuthenticators().stream())
			.filter(ai -> !((VaadinAuthentication)ai.getRetrieval()).supportsGrid())
			.map(ai -> ai.getMetadata().getId())
			.collect(Collectors.joining(" "));
	}
	
	
	private static VaadinEndpointProperties prepareConfigurationBasingOnEndpoint(Properties endpointProperties, String title)
	{
		Properties stripDown = new Properties();
		Map<Object, Object> reduced = endpointProperties.entrySet().stream()
				.filter(SandboxAuthenticationScreen::filterProperties)
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		stripDown.putAll(reduced);
		stripDown.setProperty(PREFIX + AUTHN_TITLE, title);
		stripDown.setProperty(PREFIX + AUTHN_SHOW_LAST_OPTION_ONLY, "false");
		return new VaadinEndpointProperties(stripDown);
	}

	private static boolean filterProperties(Map.Entry<Object, Object> entry)
	{
		String key = (String) entry.getKey();
		return !(key.endsWith(VaadinEndpointProperties.ENABLE_REGISTRATION) ||
				key.endsWith(VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS) ||
				key.endsWith(VaadinEndpointProperties.PRODUCTION_MODE) ||
				key.endsWith(VaadinEndpointProperties.WEB_CONTENT_PATH));
	}

	@Override
	protected VaadinAuthentication.AuthenticationCallback createFirstFactorAuthnCallback(AuthenticationOptionKey optionId,
	                                                                                     FirstFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext)
	{
		return new FirstFactorSandboxAuthnCallback(msg, interactiveAuthnProcessor, stepContext, sandboxRouter,
				new PrimaryAuthenticationListenerImpl(optionId.toStringEncodedKey(), authNPanel),
				notificationPresenter);
	}
	
	@Override
	protected VaadinAuthentication.AuthenticationCallback createSecondFactorAuthnCallback(AuthenticationOptionKey optionId,
	                                                                                      SecondFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext,
	                                                                                      PartialAuthnState partialAuthnState)
	{
		return new SecondFactorSandboxAuthnCallback(msg, interactiveAuthnProcessor, stepContext,
				new SecondaryAuthenticationListenerImpl(), 
				sandboxRouter, 
				partialAuthnState,
				notificationPresenter);
	}
	
	@Override
	protected RememberMePolicy getRememberMePolicy()
	{
		if (endpointDescription.getRealm() == null)
			return RememberMePolicy.disallow;
		return super.getRememberMePolicy();
	}
	
	@Override
	protected HorizontalLayout getRememberMeComponent(AuthenticationRealm realm)
	{
		if (realm == null)
		{
			return new HorizontalLayout();
		}

		return super.getRememberMeComponent(realm);
	}
	
	private static Dialog disabledUnknownUserProvider(UnknownRemotePrincipalResult authnResult)
	{
		throw new IllegalStateException("Showing unknown user dialog on sanbox screen - should never happen");
	}
	
	private static class NoOpCredentialRestLauncher implements CredentialResetLauncher
	{
		@Override
		public void startCredentialReset(Component credentialResetUI)
		{
		}

		@Override
		public CredentialResetUIConfig getConfiguration()
		{
			return null;
		}
	}
}
