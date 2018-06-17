/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.sandbox;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.JavaScript;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.EntityManagement;
import pl.edu.icm.unity.engine.api.authn.AuthenticationFlow;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorSupportManagement;
import pl.edu.icm.unity.engine.api.authn.SandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemoteSandboxAuthnContext;
import pl.edu.icm.unity.engine.api.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.types.authn.AuthenticationFlowDefinition;
import pl.edu.icm.unity.types.endpoint.ResolvedEndpoint;
import pl.edu.icm.unity.webui.VaadinEndpointProperties;
import pl.edu.icm.unity.webui.authn.AuthenticationUI;
import pl.edu.icm.unity.webui.authn.CancelHandler;
import pl.edu.icm.unity.webui.authn.LocaleChoiceComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.authn.WebAuthenticationProcessor;
import pl.edu.icm.unity.webui.authn.remote.UnknownUserDialog;
import pl.edu.icm.unity.webui.authn.tile.AuthNTile;
import pl.edu.icm.unity.webui.authn.tile.TileAuthenticationScreen;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Base class of Vaadin UI of the sandbox application. This servlet is based on {@link AuthenticationUI} 
 * but authenticators are overwritten with those provided by a concrete subclass.
 *  
 * @author Roman Krysinski
 */
public abstract class SandboxUIBase extends TileAuthenticationScreen 
{
	private static final String DEBUG_ID = "sbox";
	public static final String PROFILE_VALIDATION = "validate";

	private List<AuthenticationFlowDefinition> authnFlowList;
	private boolean debug;
	private boolean validationMode;
	protected AuthenticatorSupportManagement authenticatorsManagement;
	private SandboxAuthnRouter sandboxRouter;
	private List<AuthenticationFlow> authnFlows;
	
	public SandboxUIBase(UnityMessageSource msg, VaadinEndpointProperties config,
			ResolvedEndpoint endpointDescription,
			CancelHandler cancelHandler,
			EntityManagement idsMan,
			ExecutorsService execService, 
			Function<AuthenticationResult, UnknownUserDialog> unknownUserDialogProvider,
			WebAuthenticationProcessor authnProcessor,
			LocaleChoiceComponent localeChoice,
			AuthenticatorSupportManagement authenticatorsManagement,
			SandboxAuthnRouter sandboxRouter,
			List<AuthenticationFlow> authenticators)
	{
		super(msg, config, endpointDescription, () -> false, 
				() -> {}, cancelHandler, idsMan, 
				execService, false, unknownUserDialogProvider, 
				new SandboxAuthenticationProcessor(), 
				localeChoice, 
				authenticators);
		this.sandboxRouter = sandboxRouter;
		this.authnFlowList = getAllVaadinAuthenticationFlows(authenticators);
		this.authnFlows = getAuthenticationFlow(authnFlowList);
		this.authenticatorsManagement = authenticatorsManagement;
		loadAuthnFlows(authnFlows);
		config = prepareConfiguration(config.getProperties());
	}

	protected abstract List<AuthenticationFlowDefinition> getAllVaadinAuthenticationFlows(
			List<AuthenticationFlow> endpointAuthenticators);
	
	/**
	 * @return configuration of the sandbox based on the properties of the base endpoint
	 */
	protected VaadinEndpointProperties prepareConfiguration(Properties endpointProperties)
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
		return new VaadinEndpointProperties(stripDown);
	}
	
	
	@Override
	protected void init() 
	{
		if (authnFlows.size() == 0)
		{
			noRemoteAuthnUI();
			return;
		}
		
		setSandboxCallbackForAuthenticators();
		
		setHeaderTitle(msg.getMessage("SandboxUI.headerTitle"));
		
		VaadinRequest vaadinRequest = VaadinService.getCurrentRequest();
		validationMode = vaadinRequest.getParameter(PROFILE_VALIDATION) != null;
		debug = vaadinRequest.getParameter(DEBUG_ID) == null;
		customizeUI();
	}
	
	protected void customizeUI()
	{
	}
	
	private void noRemoteAuthnUI()
	{
		VerticalLayout main = new VerticalLayout();
		main.setSpacing(false);
		main.setMargin(false);
		Label errorLabel = new Label(msg.getMessage("SandboxUI.noRemoteAuthNTitle"));
		Label errorDescLabel = new Label(msg.getMessage("SandboxUI.noRemoteAuthNDesc"));
		errorLabel.addStyleName(Styles.error.toString());
		errorLabel.addStyleName(Styles.textXLarge.toString());

		Button close = new Button(msg.getMessage("close"));
		close.addClickListener(new ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				Exception errorE = new Exception(msg.getMessage("SandboxUI.noRemoteAuthNTitle"));
				RemotelyAuthenticatedInput dummy = new RemotelyAuthenticatedInput("no-idp");
				SandboxAuthnContext error = new RemoteSandboxAuthnContext(errorE, "", dummy);
				sandboxRouter.fireEvent(new SandboxAuthnEvent(error));
				JavaScript.getCurrent().execute("window.close();");
			}
		});
		main.addComponents(errorLabel, errorDescLabel, close);
		setCompositionRoot(main);
	}
	
	private class SandboxAuthnResultCallbackImpl implements SandboxAuthnResultCallback
	{
		@Override
		public void sandboxedAuthenticationDone(SandboxAuthnContext ctx)
		{
			sandboxRouter.fireEvent(new SandboxAuthnEvent(ctx));
			if (isDebug()) 
			{
				JavaScript.getCurrent().execute("window.close();");
			}
		}
	}
	
	private void setSandboxCallbackForAuthenticators() 
	{
		if (authnFlows == null)
			return;
		for (AuthNTile tile: selectorPanel.getTiles())
		{
			Collection<VaadinAuthenticationUI> authnUIs = tile.getAuthenticators().values();
			for (VaadinAuthenticationUI authUI : authnUIs)
			{
				authUI.setSandboxAuthnCallback(new SandboxAuthnResultCallbackImpl());
			}
		}
	}

	private void cancelAuthentication() 
	{
		for (AuthNTile tile: selectorPanel.getTiles())
		{
			Collection<VaadinAuthenticationUI> authnUIs = tile.getAuthenticators().values();
			for (VaadinAuthenticationUI authUI : authnUIs)
			{
				authUI.clear();
			}
		}
	}
	
	private List<AuthenticationFlow> getAuthenticationFlow(List<AuthenticationFlowDefinition> authnList) 
	{
		try
		{
			return authenticatorsManagement.getAuthenticatorUIs(authnList);
		} catch (EngineException e)
		{
			throw new IllegalStateException("Can not initialize sandbox UI", e);
		}
	}
	
	/**
	 * If sandbox URL contains {@value DEBUG_ID} then it's assumed that
	 * it works in debug mode - just for testing purposes when running sandbox
	 * "by hand" in browser.
	 * 
	 * @return false if debug mode assumed
	 */
	private boolean isDebug() 
	{
		return debug;
	}	
	
	protected boolean isProfileValidation()
	{
		return validationMode;
	}
	
	
	private void setHeaderTitle(String title) 
	{
		if (headerUIComponent != null)
		{
			headerUIComponent.setHeaderTitle(title);
		}
	}
}
