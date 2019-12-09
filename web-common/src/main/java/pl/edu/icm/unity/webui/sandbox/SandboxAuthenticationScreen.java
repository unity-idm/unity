/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

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
import java.util.Properties;
import java.util.stream.Collectors;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.column.ColumnInstantAuthenticationScreen;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

/**
 * Extends authentication screen, reconfiguring it to work as sandbox one.  
 *  
 * @author Roman Krysinski
 */
class SandboxAuthenticationScreen extends ColumnInstantAuthenticationScreen
{
	private SandboxAuthnRouter sandboxRouter;

	public SandboxAuthenticationScreen(UnityMessageSource msg, 
			ImageAccessService imageAccessService,
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, 
			SandboxAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationFlow> authenticators,
			String title,
			SandboxAuthnRouter sandboxRouter,
			boolean baseOnOriginalEndpointConfig)
	{
		super(msg, imageAccessService, baseOnOriginalEndpointConfig ? 
				prepareConfigurationBasingOnEndpoint(config.getProperties(), title) : 
				prepareFreshConfigurationWithAllOptions(title, authenticators), 
				endpointDescription, 
				() -> false,
				new NoOpCredentialRestLauncher(),
				() -> {},
				cancelHandler, idsMan, 
				execService, false, 
				SandboxAuthenticationScreen::disabledUnknownUserProvider, 
				authnProcessor, 
				localeChoice, 
				authenticators);
		this.sandboxRouter = sandboxRouter;
	}

	private static VaadinEndpointProperties prepareFreshConfigurationWithAllOptions(String title,
			List<AuthenticationFlow> authenticators)
	{
		Properties sandboxConfig = new Properties();
		sandboxConfig.setProperty(PREFIX + AUTHN_TITLE, title);
		sandboxConfig.setProperty(PREFIX + AUTHN_SHOW_LAST_OPTION_ONLY, "false");
		sandboxConfig.setProperty(PREFIX + AUTHN_ADD_ALL, "true");
		String authnsSpec = authenticators.stream().map(flow -> flow.getId()).collect(Collectors.joining(" "));
		sandboxConfig.setProperty(PREFIX + AUTHN_GRIDS_PFX + "G1." + AUTHN_GRID_CONTENTS, authnsSpec);
		sandboxConfig.setProperty(PREFIX + AUTHN_GRIDS_PFX + "G1." + AUTHN_GRID_ROWS, "15");
		sandboxConfig.setProperty(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_CONTENTS, "_GRID_G1");
		sandboxConfig.setProperty(PREFIX + AUTHN_COLUMNS_PFX + "1." + AUTHN_COLUMN_WIDTH, "28");
		return new VaadinEndpointProperties(sandboxConfig);
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
	protected void init() 
	{
		setSandboxCallbackForAuthenticators(new StandardSandboxAuthnResultCallback());
		super.init();
	}
	
	private static UnknownUserDialog disabledUnknownUserProvider(AuthenticationResult authnResult)
	{
		throw new IllegalStateException("Showing unknown user dialog on sandbox screen - should never happen");
	}
	
	private class StandardSandboxAuthnResultCallback implements SandboxAuthnResultCallback
	{
		@Override
		public void sandboxedAuthenticationDone(SandboxAuthnContext ctx)
		{
			sandboxRouter.firePartialEvent(new SandboxAuthnEvent(ctx));
		}
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
