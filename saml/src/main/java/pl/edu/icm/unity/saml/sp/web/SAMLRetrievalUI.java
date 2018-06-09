/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;

import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * The UI part of the remote SAML authn. Shows widget with a single, chosen IdP, implements 
 * authN start and awaits for answer in the context. When it is there, the validator is contacted for verification.
 * It is also possible to cancel the authentication which is in progress.
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthenticationUI
{	
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrievalUI.class);

	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private AuthenticationCallback callback;
	private SandboxAuthnResultCallback sandboxCallback;
	private String redirectParam;
	
	private String configKey;
	private String idpKey;
	private SAMLSPProperties samlProperties;
	private SamlContextManagement samlContextManagement;
	private Set<String> tags;
	
	
	private Component main;

	//TODO AUTHN handle progress/cancel
	
	public SAMLRetrievalUI(UnityMessageSource msg, SAMLExchange credentialExchange, 
			SamlContextManagement samlContextManagement, String idpKey, 
			SAMLSPProperties configurationSnapshot, String configKey)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.idpKey = idpKey;
		this.configKey = configKey;
		this.samlProperties = configurationSnapshot;
		initUI();
	}

	@Override
	public Component getComponent()
	{
		return main;
	}
	
	private void initUI()
	{
		redirectParam = installRequestHandler();
		
		ScaleMode scaleMode = samlProperties.getEnumValue(SAMLSPProperties.SELECTED_PROVDER_ICON_SCALE, 
				ScaleMode.class); 
		String name = getName();
		String logoUrl = samlProperties.getLocalizedValue(configKey + SAMLSPProperties.IDP_LOGO, msg.getLocale());
		Resource logo;
		try
		{
			logo = logoUrl == null ? Images.empty.getResource() : ImageUtils.getLogoResource(logoUrl);
		} catch (MalformedURLException e)
		{
			log.warn("Can't load logo from " + logoUrl, e);
			logo = null;
		}
		String signInLabel = msg.getMessage("AuthenticationUI.signInWith", name);
		IdPAuthNComponent idpComponent = new IdPAuthNComponent(idpKey, logo, signInLabel);
		idpComponent.addClickListener(event -> startLogin());
		idpComponent.setWidth(100, Unit.PERCENTAGE);
		this.tags = new HashSet<>(samlProperties.getListOfValues(configKey + SAMLSPProperties.IDP_NAME + "."));
		this.tags.remove(name);
		this.main = idpComponent;
	}

	private String getName()
	{
		return samlProperties.getLocalizedName(configKey, msg.getLocale());
	}
	
	private String installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof VaadinRedirectRequestHandler)
			{
				return ((VaadinRedirectRequestHandler)rh).getTriggeringParam();
			}
		}
	
		VaadinRedirectRequestHandler rh = new VaadinRedirectRequestHandler(); 
		session.addRequestHandler(rh);
		return rh.getTriggeringParam();
	}
	
	private void breakLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
			samlContextManagement.removeAuthnContext(context.getRelayState());
		}
	}
	
	private void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("WebSAMLRetrieval.loginInProgressError"));
			return;
		}
		URI requestURI = Page.getCurrent().getLocation();
		String servletPath = requestURI.getPath();
		String query = requestURI.getQuery() == null ? "" : "?" + requestURI.getQuery();
		String currentRelativeURI = servletPath + query;
		try
		{
			context = credentialExchange.createSAMLRequest(configKey, currentRelativeURI);
			context.setSandboxCallback(sandboxCallback);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("WebSAMLRetrieval.configurationError"), e);
			log.error("Can not create SAML request", e);
			breakLogin();
			return;
		}
		
		callback.onStartedAuthentication(AuthenticationStyle.WITH_EXTERNAL_CANCEL);
		session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
		samlContextManagement.addAuthnContext(context);
		
		Page.getCurrent().open(servletPath + "?" + redirectParam, null);
	}

	/**
	 * Called when a SAML response is received.
	 * @param authnContext
	 */
	private void onSamlAnswer(RemoteAuthnContext authnContext)
	{
		AuthenticationResult authnResult;
		String reason = null;
		Exception savedException = null;
		
		try
		{
			authnResult = credentialExchange.verifySAMLResponse(authnContext);
		} catch (AuthenticationException e)
		{
			savedException = e;
			reason = NotificationPopup.getHumanMessage(e, "<br>");
			authnResult = e.getResult();
		} catch (Exception e)
		{
			log.error("Runtime error during SAML response processing or principal mapping", e);
			authnResult = new AuthenticationResult(Status.deny, null);
		}

		if (authnContext.getRegistrationFormForUnknown() != null)
		{
			log.debug("Enabling registration component");
			authnResult.setFormForUnknownPrincipal(authnContext.getRegistrationFormForUnknown());
		}
		authnResult.setEnableAssociation(authnContext.isEnableAssociation());
		
		
		if (authnResult.getStatus() == Status.success || 
				authnResult.getStatus() == Status.unknownRemotePrincipal)
		{
			breakLogin();
			callback.onCompletedAuthentication(authnResult);
		} else
		{
			if (savedException != null)
				log.warn("SAML response verification or processing failed", savedException);
			else
				log.warn("SAML response verification or processing failed");
			breakLogin();
			Optional<String> errorDetail = reason == null ? Optional.empty() : 
				Optional.of(msg.getMessage("WebSAMLRetrieval.authnFailedDetailInfo", reason));
			String error = msg.getMessage("WebSAMLRetrieval.authnFailedError");
			callback.onFailedAuthentication(authnResult, error, errorDetail);
		}
	}
	
	@Override
	public void setAuthenticationCallback(AuthenticationCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		WrappedSession session = request.getWrappedSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.trace("Either user refreshes page, or different authN arrived");
		} else if (context.getResponse() == null)
		{
			log.debug("Authentication started but SAML response not arrived (user back button)");
		} else 
		{
			onSamlAnswer(context);
		}
	}

	@Override
	public String getLabel()
	{	
		return getName();
	}

	@Override
	public Resource getImage()
	{
		String url = samlProperties.getLocalizedValue(configKey + SAMLSPProperties.IDP_LOGO, msg.getLocale());
		if (url == null)
			return null;
		try
		{
			return ImageUtils.getLogoResource(url);
		} catch (MalformedURLException e)
		{
			log.error("Invalid logo URL " + url, e);
			return null;
		}
	}

	@Override
	public void clear()
	{
		breakLogin();
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