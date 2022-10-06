/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import com.vaadin.server.*;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.Component;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationStepContext;
import pl.edu.icm.unity.engine.api.authn.RememberMeToken.LoginMachineDetails;
import pl.edu.icm.unity.saml.metadata.cfg.ExternalLogoFileLoader;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.saml.sp.config.TrustedIdPKey;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.UrlHelper;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.authn.IdPAuthNGridComponent;
import pl.edu.icm.unity.webui.authn.LoginMachineDetailsExtractor;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

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
	private final AuthenticationStepContext authenticationStepContext;
	private final SAMLExchange credentialExchange;
	private final TrustedIdPKey configKey;
	private final String idpKey;
	private final SamlContextManagement samlContextManagement;
	
	private IdPVisalSettings configuration;
	private Set<String> tags;
	private Component main;
	private final Context context;
	private IdPAuthNComponent idpComponent;
	private AuthenticationCallback callback;
	private String redirectParam;
	private ExternalLogoFileLoader externalLogoFileLoader;

	public SAMLRetrievalUI(MessageSource msg, SAMLExchange credentialExchange,
			SamlContextManagement samlContextManagement, TrustedIdPKey configKey,
			Context context, AuthenticationStepContext authenticationStepContext, ExternalLogoFileLoader externalLogoFileLoader)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.idpKey = authenticationStepContext.authnOptionId.getOptionKey();
		this.configKey = configKey;
		this.authenticationStepContext = authenticationStepContext;
		this.configuration = credentialExchange.getVisualSettings(configKey, msg.getLocale());
		this.context = context;
		this.externalLogoFileLoader = externalLogoFileLoader;
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

	void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		startFreshLogin(session);
	}

	private void startFreshLogin(WrappedSession session)
	{
		String currentRelativeURI = UrlHelper.getCurrentVaadingRelativeURI();
		RemoteAuthnContext context;
		try
		{
			LoginMachineDetails loginMachineDetails = LoginMachineDetailsExtractor.getLoginMachineDetailsFromCurrentRequest();
			context = credentialExchange.createSAMLRequest(configKey, currentRelativeURI, 
					authenticationStepContext, 
					loginMachineDetails, currentRelativeURI, callback.getTriggeringContext());
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("WebSAMLRetrieval.configurationError"), e);
			log.error("Can not create SAML request", e);
			clear();
			return;
		}
		log.info("Starting remote SAML authn, current relative URI is {}", currentRelativeURI);
		idpComponent.setEnabled(false);
		callback.onStartedAuthentication();
		session.setAttribute(VaadinRedirectRequestHandler.REMOTE_AUTHN_CONTEXT, context);
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
			return null;
		if(configuration.logoURI.startsWith("file:"))
		{
			File sourceFile = new File(configuration.logoURI);
			if(sourceFile.exists())
				return new IdPAuthNComponent.DisappearingFileResource(sourceFile);
			return null;
		}
		try
		{
			return externalLogoFileLoader.getFile(configuration.federationId, configKey,
					VaadinService.getCurrentRequest().getLocale())
				.map(IdPAuthNComponent.DisappearingFileResource::new)
				.orElse(null);
		} catch (Exception e)
		{
			log.debug("Can not load logo fetched from URI " + configuration.logoURI, e);
			return null;
		}
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