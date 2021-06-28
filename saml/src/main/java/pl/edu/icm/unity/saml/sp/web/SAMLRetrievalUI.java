/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.ExternalResource;
import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.UI;

import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.files.URIAccessService;
import pl.edu.icm.unity.engine.api.files.URIHelper;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.UrlHelper;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.authn.IdPAuthNGridComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.FileStreamResource;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * The UI part of the remote SAML authn. Shows widget with a single, chosen IdP,
 * implements authN start and awaits for answer in the context. When it is
 * there, the validator is contacted for verification. It is also possible to
 * cancel the authentication which is in progress.
 * 
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthenticationUI
{
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrievalUI.class);

	private final MessageSource msg;
	private final URIAccessService uriAccessService;
	private final AuthenticationStepContext authenticationStepContext;
	private final SAMLExchange credentialExchange;
	private final String configKey;
	private final String idpKey;
	private final SamlContextManagement samlContextManagement;
	
	private IdPVisalSettings configuration;
	private Set<String> tags;
	private Component main;
	private final Context context;
	private IdPAuthNComponent idpComponent;
	private AuthenticationCallback callback;
	private SandboxAuthnResultCallback sandboxCallback;
	private String redirectParam;

	public SAMLRetrievalUI(MessageSource msg, URIAccessService uriAccessService, SAMLExchange credentialExchange,
			SamlContextManagement samlContextManagement, String configKey,
			Context context, AuthenticationStepContext authenticationStepContext)
	{
		this.msg = msg;
		this.uriAccessService = uriAccessService;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.idpKey = authenticationStepContext.authnOptionId.getOptionKey();
		this.configKey = configKey;
		this.authenticationStepContext = authenticationStepContext;
		this.configuration = credentialExchange.getVisualSettings(configKey, msg.getLocale());
		this.context = context;
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
		idpComponent.setWidth(100, Unit.PERCENTAGE);
		return idpComponent;
	}

	private void initUI()
	{
		redirectParam = installRequestHandler();
		Resource logo;
		if (configuration.logoURI == null)
		{
			logo = Images.empty.getResource();
		}
		else
		{
			logo = getImage();
		}
		
		String signInLabel;
		if (context == Context.LOGIN)
			signInLabel = msg.getMessage("AuthenticationUI.signInWith", configuration.name);
		else
			signInLabel = msg.getMessage("AuthenticationUI.signUpWith", configuration.name);
		idpComponent = new IdPAuthNComponent(getRetrievalClassName(), logo, signInLabel);
		idpComponent.addClickListener(event -> startLogin());
		idpComponent.setWidth(100, Unit.PERCENTAGE);	
					
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

	private void breakLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session
				.getAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
			samlContextManagement.removeAuthnContext(context.getRelayState());
		}
	}

	void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session
				.getAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			ConfirmDialog confirmKillingPreviousAuthn = new ConfirmDialog(msg,
					msg.getMessage("WebSAMLRetrieval.breakLoginInProgressConfirm"), () -> {
						breakLogin();
						startFreshLogin(session);
					});
			confirmKillingPreviousAuthn.setHTMLContent(true);
			confirmKillingPreviousAuthn.setSizeEm(35, 20);
			confirmKillingPreviousAuthn.show();
			return;
		}
		startFreshLogin(session);
	}

	private void startFreshLogin(WrappedSession session)
	{
		String currentRelativeURI = UrlHelper.getCurrentRelativeURI();
		RemoteAuthnContext context;
		try
		{
			context = credentialExchange.createSAMLRequest(configKey, currentRelativeURI, 
					authenticationStepContext, callback.isSetRememberMe());
			context.setSandboxCallback(sandboxCallback);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("WebSAMLRetrieval.configurationError"), e);
			log.error("Can not create SAML request", e);
			clear();
			return;
		}
		log.info("Starting remote SAML authn, current relative URI is {}", currentRelativeURI);
		idpComponent.setEnabled(false);
		callback.onStartedAuthentication(AuthenticationStyle.WITH_EXTERNAL_CANCEL);
		session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
		samlContextManagement.addAuthnContext(context);

		URI requestURI = Page.getCurrent().getLocation();
		String servletPath = requestURI.getPath();
		Page.getCurrent().open(servletPath + "?" + redirectParam, null);
	}

	@Override
	public void setAuthenticationCallback(AuthenticationCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public String getLabel()
	{
		return configuration.name;
	}

	@Override
	public Resource getImage()
	{
		if (configuration.logoURI == null)
		{
			return null;
		}

		try
		{
			URI uri = URIHelper.parseURI(configuration.logoURI);
			if (URIHelper.isWebReady(uri))
			{
				return new ExternalResource(uri.toString());
			} else
			{
				return new FileStreamResource(
						uriAccessService.readImageURI(uri, UI.getCurrent().getTheme()))
								.getResource();
			}
		} catch (Exception e)
		{
			log.error("Invalid logo URI " + configuration.logoURI, e);
			return null;
		}
	}
	
	@Override
	public void clear()
	{
		breakLogin();
		idpComponent.setEnabled(true);
	}

	@Override
	public void setSandboxAuthnCallback(SandboxAuthnResultCallback callback)
	{
		sandboxCallback = callback;
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