/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
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
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.UnexpectedIdentityException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.types.authn.ExpectedIdentity;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.UrlHelper;
import pl.edu.icm.unity.webui.authn.CommonWebAuthnProperties;
import pl.edu.icm.unity.webui.authn.IdPAuthNComponent;
import pl.edu.icm.unity.webui.authn.IdPAuthNGridComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationStyle;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.Context;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ConfirmDialog;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;

/**
 * UI part of OAuth retrieval. Shows a single provider, redirects to it if requested.
 * @author K. Benedyczak
 */
public class OAuth2RetrievalUI implements VaadinAuthenticationUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2RetrievalUI.class);
	
	private UnityMessageSource msg;
	private OAuthExchange credentialExchange;
	private OAuthContextsManagement contextManagement;
	private final String configKey;
	private final String idpKey;
	
	private AuthenticationCallback callback;
	private SandboxAuthnResultCallback sandboxCallback;
	private String redirectParam;

	private Component main;
	private String authenticatorName;

	private IdPAuthNComponent idpComponent;
	private Context context;

	private ExpectedIdentity expectedIdentity;

	public OAuth2RetrievalUI(UnityMessageSource msg, OAuthExchange credentialExchange,
			OAuthContextsManagement contextManagement, ExecutorsService executorsService, 
			String idpKey, String configKey, String authenticatorName, Context context)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.contextManagement = contextManagement;
		this.idpKey = idpKey;
		this.configKey = configKey;
		this.authenticatorName = authenticatorName;
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
		String logoUrl = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, 
				msg.getLocale());
		Resource logo;
		try
		{
			logo = logoUrl == null ? Images.empty.getResource() : ImageUtils.getLogoResource(logoUrl);
		} catch (MalformedURLException e)
		{
			log.warn("Can't load logo from " + logoUrl, e);
			logo = null;
		}
		
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
		return authenticatorName + "." + idpKey;
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
		String url = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, 
				msg.getLocale());
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
	
	private void breakLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
			contextManagement.removeAuthnContext(context.getRelayState());
		}
	}
	
	private void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			ConfirmDialog confirmKillingPreviousAuthn = new ConfirmDialog(msg, 
					msg.getMessage("OAuth2Retrieval.breakLoginInProgressConfirm"), 
					() -> {
						breakLogin();
						startFreshLogin(session);	
					});
			confirmKillingPreviousAuthn.setSizeEm(35, 20);
			confirmKillingPreviousAuthn.setHTMLContent(true);
			confirmKillingPreviousAuthn.show();
			return;
		}
		startFreshLogin(session);
	}

	private void startFreshLogin(WrappedSession session)
	{
		try
		{
			OAuthContext context = credentialExchange.createRequest(configKey, Optional.ofNullable(expectedIdentity));
			idpComponent.setEnabled(false);
			callback.onStartedAuthentication(AuthenticationStyle.WITH_EXTERNAL_CANCEL);
			String currentRelativeURI = UrlHelper.getCurrentRelativeURI();
			context.setReturnUrl(currentRelativeURI);
			session.setAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT, context);
			context.setSandboxCallback(sandboxCallback);
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

	
	/**
	 * Called when a OAuth authorization code response is received.
	 */
	private void onAuthzAnswer(OAuthContext authnContext)
	{
		log.debug("RetrievalUI received OAuth response");
		AuthenticationResult authnResult;
		
		String reason = null;
		String error = null;
		AuthenticationException savedException = null;
		try
		{
			authnResult = credentialExchange.verifyOAuthAuthzResponse(authnContext);
		} catch (AuthenticationException e)
		{
			savedException = e;
			reason = NotificationPopup.getHumanMessage(e, "<br>");
			authnResult = e.getResult();
		} catch (UnexpectedIdentityException uie)
		{
			error = msg.getMessage("OAuth2Retrieval.unexpectedUser", uie.expectedIdentity);
			authnResult = new AuthenticationResult(Status.deny, null);
		} catch (Exception e)
		{
			log.error("Runtime error during OAuth2 response processing or principal mapping", e);
			authnResult = new AuthenticationResult(Status.deny, null);
		}
		
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(
				authnContext.getProviderConfigKey()); 
		String regFormForUnknown = providerProps.getValue(CommonWebAuthnProperties.REGISTRATION_FORM);
		if (regFormForUnknown != null)
			authnResult.setFormForUnknownPrincipal(regFormForUnknown);
		boolean enableAssociation = providerProps.isSet(CommonWebAuthnProperties.ENABLE_ASSOCIATION) ?
				providerProps.getBooleanValue(CommonWebAuthnProperties.ENABLE_ASSOCIATION) :
				clientProperties.getBooleanValue(CommonWebAuthnProperties.DEF_ENABLE_ASSOCIATION);
		authnResult.setEnableAssociation(enableAssociation);
		
		if (authnResult.getStatus() == Status.success)
		{
			breakLogin();
			callback.onCompletedAuthentication(authnResult);
		} else if (authnResult.getStatus() == Status.unknownRemotePrincipal)
		{
			clear();
			callback.onCompletedAuthentication(authnResult);
		} else
		{
			if (savedException != null)
				log.debug("OAuth2 authorization code verification or processing failed", 
						savedException);
			else
				log.debug("OAuth2 authorization code verification or processing failed");
			Optional<String> errorDetail = reason == null ? Optional.empty() : 
				Optional.of(msg.getMessage("OAuth2Retrieval.authnFailedDetailInfo", reason));
			if (error == null)
				error = msg.getMessage("OAuth2Retrieval.authnFailedError");
			clear();
			callback.onFailedAuthentication(authnResult, error, errorDetail);
		}
	}

	@Override
	public void refresh(VaadinRequest request) 
	{
		WrappedSession session = request.getWrappedSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.trace("Either user refreshes page, or different authN arrived");
		} else if (!context.isAnswerPresent()) 
		{
			log.debug("Authentication started but OAuth2 response not arrived (user back button)");
		} else 
		{
			onAuthzAnswer(context);
		}
	}

	@Override
	public void setSandboxAuthnCallback(SandboxAuthnResultCallback callback) 
	{
		this.sandboxCallback = callback;
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
