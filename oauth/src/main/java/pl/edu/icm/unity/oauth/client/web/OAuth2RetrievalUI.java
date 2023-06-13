/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.VaadinLogoImageLoader;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.authn.ExpectedIdentity;
import pl.edu.icm.unity.base.identity.Entity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import io.imunity.vaadin.auth.LoginMachineDetailsExtractor;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.idp.IdPAuthNComponent;
import io.imunity.vaadin.auth.idp.IdPAuthNGridComponent;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

/**
 * UI part of OAuth retrieval. Shows a single provider, redirects to it if requested.
 * @author K. Benedyczak
 */
public class OAuth2RetrievalUI implements VaadinAuthentication.VaadinAuthenticationUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2RetrievalUI.class);
	private static final String SELF_WINDOW_NAME = "_self";

	private final MessageSource msg;
	private final VaadinLogoImageLoader imageAccessService;
	private final OAuthExchange credentialExchange;
	private final String configKey;
	private final String idpKey;
	private final VaadinAuthentication.Context context;
	private final AuthenticationStepContext authenticationStepContext;
	private final NotificationPresenter notificationPresenter;

	private VaadinAuthentication.AuthenticationCallback callback;
	private String redirectParam;

	private VerticalLayout main;

	private IdPAuthNComponent idpComponent;

	private ExpectedIdentity expectedIdentity;


	public OAuth2RetrievalUI(MessageSource msg, VaadinLogoImageLoader imageAccessService, OAuthExchange credentialExchange,
	                         String configKey, VaadinAuthentication.Context context,
	                         AuthenticationStepContext authenticationStepContext,
	                         NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.imageAccessService = imageAccessService;
		this.credentialExchange = credentialExchange;
		this.idpKey = authenticationStepContext.authnOptionId.getOptionKey();;
		this.configKey = configKey;
		this.context = context;
		this.authenticationStepContext = authenticationStepContext;
		this.notificationPresenter = notificationPresenter;
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
		idpComponent.addButtonClickListener(event -> startLogin());
		idpComponent.setWidthFull();
		return idpComponent;
	}
	
	private void initUI()
	{
		redirectParam = installRequestHandler();

		OAuthClientProperties clientProperties = credentialExchange.getSettings();

		CustomProviderProperties providerProps = clientProperties.getProvider(configKey);
		String name = providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, msg.getLocale());
		String logoURI = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, msg.getLocale());

		Image logo = imageAccessService.loadImageFromUri(logoURI)
				.orElse(new Image());
		logo.getStyle().set("max-height", "1.5rem");
		logo.getStyle().set("padding-top", "0.25em");

		String signInLabel;
		if (context == VaadinAuthentication.Context.LOGIN)
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
	public void setAuthenticationCallback(VaadinAuthentication.AuthenticationCallback callback)
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
	public Image getImage()
	{
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(configKey);
		String logoURI = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, msg.getLocale());
		return imageAccessService.loadImageFromUri(logoURI).orElse(null);
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
		UI.getCurrent().getPage().fetchCurrentURL(currentRelativeURI ->
		{
			try
			{
				LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
				OAuthContext context = credentialExchange.createRequest(configKey, Optional.ofNullable(expectedIdentity),
						authenticationStepContext, loginMachineDetails,
						currentRelativeURI.getPath(), callback.getTriggeringContext());
				idpComponent.setEnabled(false);
				callback.onStartedAuthentication();
				String path = currentRelativeURI.getPath() + (currentRelativeURI.getQuery() != null ? "?" + currentRelativeURI.getQuery() : "");
				context.setReturnUrl(path);
				session.setAttribute(RedirectRequestHandler.REMOTE_AUTHN_CONTEXT, context);
			} catch (Exception e)
			{
				notificationPresenter.showError(msg.getMessage("OAuth2Retrieval.configurationError"), e.getMessage());
				log.error("Can not create OAuth2 request", e);
				clear();
				return;
			}
			UI.getCurrent().getPage().open(currentRelativeURI.getPath() + "?" + redirectParam, SELF_WINDOW_NAME);
	});
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
