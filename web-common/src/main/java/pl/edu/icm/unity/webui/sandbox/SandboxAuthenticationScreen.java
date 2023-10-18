/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import static com.google.common.base.Preconditions.checkNotNull;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_ADD_ALL;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMNS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_COLUMN_WIDTH;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRIDS_PFX;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_CONTENTS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_GRID_ROWS;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_SHOW_LAST_OPTION_ONLY;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.AUTHN_TITLE;
import static pl.edu.icm.unity.webui.VaadinEndpointProperties.PREFIX;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;

import pl.edu.icm.unity.base.authn.AuthenticationOptionKey;
import pl.edu.icm.unity.base.authn.AuthenticationRealm;
import pl.edu.icm.unity.base.authn.RememberMePolicy;
import pl.edu.icm.unity.base.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.InteractiveAuthenticationProcessorEE8;
import pl.edu.icm.unity.engine.api.authn.PartialAuthnState;
import pl.edu.icm.unity.engine.api.authn.RemoteAuthenticationResult.UnknownRemotePrincipalResult;
import pl.edu.icm.unity.engine.api.authn.sandbox.SandboxAuthnRouter;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen;
import pl.edu.icm.unity.webui.authn.column.FirstFactorAuthNPanel;
import pl.edu.icm.unity.webui.authn.column.SecondFactorAuthNPanel;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Extends authentication screen, reconfiguring it to work as sandbox one.  
 *  
 * @author Roman Krysinski
 */
public class SandboxAuthenticationScreen extends ColumnInstantAuthenticationScreen
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SandboxAuthenticationScreen.class);
	private final SandboxAuthnRouter sandboxRouter;

	public SandboxAuthenticationScreen(MessageSource msg, 
			ImageAccessService imageAccessService,
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, 
			InteractiveAuthenticationProcessorEE8 authnProcessor,
			Optional<LocaleChoiceComponent> localeChoice,
			List<AuthenticationFlow> authenticators,
			String title,
			SandboxAuthnRouter sandboxRouter,
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
				authnProcessor);
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
					.filter(authenticator -> ((VaadinAuthentication)authenticator.getRetrieval()).supportsGrid())
					.findAny().isPresent())
			.map(flow -> flow.getId())
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
		Map<Object, Object> reduced = endpointProperties.entrySet().stream().filter(entry -> {
			String key = (String) entry.getKey();
			return !(key.endsWith(VaadinEndpointProperties.ENABLE_REGISTRATION) || 
					key.endsWith(VaadinEndpointProperties.ENABLED_REGISTRATION_FORMS) ||
					key.endsWith(VaadinEndpointProperties.PRODUCTION_MODE) ||
					key.endsWith(VaadinEndpointProperties.WEB_CONTENT_PATH));
		}).collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
		stripDown.putAll(reduced);
		stripDown.setProperty(PREFIX + AUTHN_TITLE, title);
		stripDown.setProperty(PREFIX + AUTHN_SHOW_LAST_OPTION_ONLY, "false");
		return new VaadinEndpointProperties(stripDown);
	}

	@Override
	protected AuthenticationCallback createFirstFactorAuthnCallback(AuthenticationOptionKey optionId,
			FirstFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext)
	{
		return new FirstFactorSandboxAuthnCallback(msg, interactiveAuthnProcessor, stepContext, sandboxRouter,
				new PrimaryAuthenticationListenerImpl(optionId.toStringEncodedKey(), authNPanel));
	}
	
	@Override
	protected AuthenticationCallback createSecondFactorAuthnCallback(AuthenticationOptionKey optionId,
			SecondFactorAuthNPanel authNPanel, AuthenticationStepContext stepContext, 
			PartialAuthnState partialAuthnState)
	{
		return new SecondFactorSandboxAuthnCallback(msg, interactiveAuthnProcessor, stepContext, 
				new SecondaryAuthenticationListenerImpl(), 
				sandboxRouter, 
				partialAuthnState);
	}
	
	@Override
	protected RememberMePolicy getRememberMePolicy()
	{
		if (endpointDescription.getRealm() == null)
			return RememberMePolicy.disallow;
		return super.getRememberMePolicy();
	}
	
	@Override
	protected Component getRememberMeComponent(AuthenticationRealm realm)
	{
		if (realm == null)
		{
			return new HorizontalLayout();
		}

		return super.getRememberMeComponent(realm);
	}
	
	private static UnknownUserDialog disabledUnknownUserProvider(UnknownRemotePrincipalResult authnResult)
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
