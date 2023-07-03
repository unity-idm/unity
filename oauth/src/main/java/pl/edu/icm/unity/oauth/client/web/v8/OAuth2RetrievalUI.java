/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web.v8;

import com.vaadin.server.*;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.authn.ExpectedIdentity;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.webui.UrlHelper;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.authn.IdPAuthNGridComponent;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.file.ImageAccessService;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * UI part of OAuth retrieval. Shows a single provider, redirects to it if requested.
 * @author K. Benedyczak
 */
public class OAuth2RetrievalUI implements VaadinAuthenticationUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2RetrievalUI.class);
	
	private final MessageSource msg;
	private final ImageAccessService imageAccessService;
	private final OAuthExchange credentialExchange;
	private final String configKey;
	private final String idpKey;
	private final Context context;
	private final AuthenticationStepContext authenticationStepContext;
	
	private AuthenticationCallback callback;
	private String redirectParam;

	private Component main;

	private IdPAuthNComponent idpComponent;

	private ExpectedIdentity expectedIdentity;


	public OAuth2RetrievalUI(MessageSource msg, ImageAccessService imageAccessService, OAuthExchange credentialExchange,
			ExecutorsService executorsService, 
			String configKey, Context context, 
			AuthenticationStepContext authenticationStepContext)
	{
		this.msg = msg;
		this.imageAccessService = imageAccessService;
		this.credentialExchange = credentialExchange;
		this.idpKey = authenticationStepContext.authnOptionId.getOptionKey();;
		this.configKey = configKey;
		this.context = context;
		this.authenticationStepContext = authenticationStepContext;
		initUI();
	}

	@Override
	public Component getComponent()
	{
		return main;
	}
	
	@Override
	public Component getGridCompatibleComponent()
	{
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(configKey);
		String name = providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, msg.getLocale());
		IdPAuthNGridComponent idpComponent = new IdPAuthNGridComponent(getRetrievalClassName(), name);
		idpComponent.addClickListener(event -> startLogin());
		idpComponent.setWidth(100, Unit.PERCENTAGE);
		return idpComponent;
	}
	
	private void initUI()
	{
		redirectParam = installRequestHandler();

		OAuthClientProperties clientProperties = credentialExchange.getSettings();

		CustomProviderProperties providerProps = clientProperties.getProvider(configKey);
		String name = providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, msg.getLocale());
		String logoURI = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, msg.getLocale());

		Resource logo = imageAccessService.getConfiguredImageResourceFromNullableUri(logoURI)
				.orElse(Images.empty.getResource());

		String signInLabel;
		if (context == Context.LOGIN)
			signInLabel = msg.getMessage("AuthenticationUI.signInWith", name);
		else
			signInLabel = msg.getMessage("AuthenticationUI.signUpWith", name);
		idpComponent = new IdPAuthNComponent(getRetrievalClassName(), logo, signInLabel);
		idpComponent.addClickListener(event -> startLogin());
		main = idpComponent;
	}

	private String getRetrievalClassName()
	{
		return authenticationStepContext.authnOptionId.getAuthenticatorKey() + "." + idpKey;
	}
	
	@Override
	public void setAuthenticationCallback(AuthenticationCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public String getLabel()
	{
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(configKey);
		return providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, msg.getLocale());
	}

	@Override
	public Resource getImage()
	{
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(configKey);
		String logoURI = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, msg.getLocale());
		return imageAccessService.getConfiguredImageResourceFromNullableUri(logoURI).orElse(null);
	}

	@Override
	public void clear()
	{
		idpComponent.setEnabled(true);
	}
	
	private String installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof RedirectRequestHandler)
			{
				return ((RedirectRequestHandler)rh).getTriggeringParam();
			}
		}
	
		RedirectRequestHandler rh = new RedirectRequestHandler(); 
		session.addRequestHandler(rh);
		return rh.getTriggeringParam();
	}
	
	void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		startFreshLogin(session);
	}

	private void startFreshLogin(WrappedSession session)
	{
		try
		{
			String currentRelativeURI = UrlHelper.getCurrentVaadingRelativeURI();
			LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
			OAuthContext context = credentialExchange.createRequest(configKey, Optional.ofNullable(expectedIdentity),
					authenticationStepContext, loginMachineDetails,  
					currentRelativeURI, callback.getTriggeringContext());
			idpComponent.setEnabled(false);
			callback.onStartedAuthentication();
			context.setReturnUrl(currentRelativeURI);
			session.setAttribute(RedirectRequestHandler.REMOTE_AUTHN_CONTEXT, context);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("OAuth2Retrieval.configurationError"), e);
			log.error("Can not create OAuth2 request", e);
			clear();
			return;
		}
		
		URI requestURI = Page.getCurrent().getLocation();
		String servletPath = requestURI.getPath();
		Page.getCurrent().open(servletPath + "?" + redirectParam, null);
	}

	@Override
	public String getId()
	{
		return idpKey;
	}

	@Override
	public void presetEntity(Entity authenticatedEntity)
	{
	}
	
	@Override
	public void setExpectedIdentity(ExpectedIdentity expectedIdentity)
	{
		this.expectedIdentity = expectedIdentity;
	}

	@Override
	public Set<String> getTags()
	{
		return Collections.emptySet();
	}
}
