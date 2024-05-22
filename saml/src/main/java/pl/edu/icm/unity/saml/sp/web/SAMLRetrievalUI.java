/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.RequestHandler;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WrappedSession;

import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.idp.IdPAuthNComponent;
import io.imunity.vaadin.auth.idp.IdPAuthNGridComponent;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.LoginMachineDetailsExtractor;
import io.imunity.vaadin.endpoint.common.SessionStorage;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;

/**
 * The UI part of the remote SAML authn. Shows widget with a single, chosen IdP,
 * implements authN start and awaits for answer in the context. When it is
 * there, the validator is contacted for verification. It is also possible to
 * cancel the authentication which is in progress.
 * 
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthentication.VaadinAuthenticationUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrievalUI.class);
	private static final String SELF_WINDOW_NAME = "_self";

	private final MessageSource msg;
	private final AuthenticationStepContext authenticationStepContext;
	private final SAMLExchange credentialExchange;
	private final TrustedIdPKey configKey;
	private final String idpKey;
	private final SamlContextManagement samlContextManagement;
	private final LogoExposingService logoExposingService;
	private final NotificationPresenter notificationPresenter;

	private final IdPVisalSettings configuration;
	private Set<String> tags;
	private Component main;
	private final VaadinAuthentication.Context context;
	private IdPAuthNComponent idpComponent;
	private VaadinAuthentication.AuthenticationCallback callback;
	private String redirectParam;

	public SAMLRetrievalUI(MessageSource msg, SAMLExchange credentialExchange,
	                       SamlContextManagement samlContextManagement, TrustedIdPKey configKey,
	                       VaadinAuthentication.Context context, AuthenticationStepContext authenticationStepContext,
	                       LogoExposingService logoExposingService, NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.idpKey = authenticationStepContext.authnOptionId.getOptionKey();
		this.configKey = configKey;
		this.authenticationStepContext = authenticationStepContext;
		this.configuration = credentialExchange.getVisualSettings(configKey, msg.getLocale());
		this.context = context;
		this.logoExposingService = logoExposingService;
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
		IdPAuthNGridComponent idpComponent = new IdPAuthNGridComponent(getRetrievalClassName(),
				configuration.name);
		idpComponent.addClickListener(event -> startLogin());
		idpComponent.setWidthFull();
		return idpComponent;
	}

	private void initUI()
	{
		redirectParam = installRequestHandler();
		Image logo = getImage();
		if (logo == null)
		{
			logo = new Image();
		}
		logo.setClassName("u-logo-idp-image");

		String signInLabel;
		if (context == VaadinAuthentication.Context.LOGIN)
		{
			signInLabel = msg.getMessage("AuthenticationUI.signInWith", configuration.name);
		} else
		{
			signInLabel = msg.getMessage("AuthenticationUI.signUpWith", configuration.name);
		}
		idpComponent = new IdPAuthNComponent(getRetrievalClassName(), logo, signInLabel);
		idpComponent.addClickListener(event -> startLogin());
		idpComponent.setWidthFull();
					
		this.tags = new HashSet<>(configuration.tags);
		this.tags.remove(configuration.name);
		this.main = idpComponent;
	}

	private String getRetrievalClassName()
	{
		return authenticationStepContext.authnOptionId.getAuthenticatorKey() + "." + idpKey;
	}

	private String installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		for (RequestHandler rh : requestHandlers)
		{
			if (rh instanceof VaadinRedirectRequestHandler)
			{
				return ((VaadinRedirectRequestHandler) rh).getTriggeringParam();
			}
		}

		VaadinRedirectRequestHandler rh = new VaadinRedirectRequestHandler();
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
		SessionStorage.consumeRedirectUrl((ultimateReturnURL, currentRelativeURI) ->
		{
			RemoteAuthnContext context;
			String path = currentRelativeURI.getPath() + (currentRelativeURI.getQuery() != null ? "?" + currentRelativeURI.getQuery() : "");
			try
			{
				LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
				context = credentialExchange.createSAMLRequest(configKey, path,
						authenticationStepContext,
						loginMachineDetails, ultimateReturnURL, callback.getTriggeringContext());
			} catch (Exception e)
			{
				notificationPresenter.showError(msg.getMessage("WebSAMLRetrieval.configurationError"), e.getMessage());
				log.error("Can not create SAML request", e);
				clear();
				return;
			}
			log.info("Starting remote SAML authn, current relative URI is {}", currentRelativeURI);
			idpComponent.setEnabled(false);
			callback.onStartedAuthentication();
			session.setAttribute(VaadinRedirectRequestHandler.REMOTE_AUTHN_CONTEXT, context);
			samlContextManagement.addAuthnContext(context);

			UI.getCurrent().getPage().open(path + "?" + redirectParam, SELF_WINDOW_NAME);
		});
	}

	@Override
	public void setAuthenticationCallback(VaadinAuthentication.AuthenticationCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public String getLabel()
	{
		return configuration.name;
	}

	@Override
	public Image getImage()
	{
		return logoExposingService.getAsResource(configuration, configKey);
	}
	
	@Override
	public void clear()
	{
		idpComponent.setEnabled(true);
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
	public Set<String> getTags()
	{
		return tags;
	}
}