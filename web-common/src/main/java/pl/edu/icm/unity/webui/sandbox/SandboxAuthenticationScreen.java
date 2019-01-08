/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

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

/**
 * Extends authentication screen, reconfiguring it to work as sandbox one.  
 *  
 * @author Roman Krysinski
 */
class SandboxAuthenticationScreen extends ColumnInstantAuthenticationScreen
{
	private SandboxAuthnRouter sandboxRouter;

	public SandboxAuthenticationScreen(UnityMessageSource msg, 
			VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, 
			SandboxAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			List<AuthenticationFlow> authenticators,
			String title,
			SandboxAuthnRouter sandboxRouter)
	{
		super(msg, prepareConfiguration(config.getProperties(), title), 
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

	/**
	 * @return configuration of the sandbox based on the properties of the base endpoint
	 */
	private static VaadinEndpointProperties prepareConfiguration(Properties endpointProperties, String title)
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
