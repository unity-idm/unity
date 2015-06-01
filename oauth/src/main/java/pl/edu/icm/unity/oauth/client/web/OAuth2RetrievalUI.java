/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collection;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.VaadinEndpointProperties.ScaleMode;
import pl.edu.icm.unity.webui.authn.IdPROComponent;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

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
	private String idpKey;
	
	private AuthenticationResultCallback callback;
	private SandboxAuthnResultCallback sandboxCallback;
	private String redirectParam;

	private Label messageLabel;
	private HtmlSimplifiedLabel errorDetailLabel;
	
	private Component main;
	
	public OAuth2RetrievalUI(UnityMessageSource msg, OAuthExchange credentialExchange,
			OAuthContextsManagement contextManagement, ExecutorsService executorsService, String idpKey)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.contextManagement = contextManagement;
		this.idpKey = idpKey;
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

		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);

		ScaleMode scaleMode = clientProperties.getEnumValue(OAuthClientProperties.SELECTED_ICON_SCALE, 
				ScaleMode.class); 
		CustomProviderProperties providerProps = clientProperties.getProvider(idpKey);
		String name = providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, msg.getLocale());
		String logoUrl = providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, 
				msg.getLocale());
		IdPROComponent idpComponent = new IdPROComponent(logoUrl, name, scaleMode);
		
		ret.addComponent(idpComponent);
		ret.setComponentAlignment(idpComponent, Alignment.TOP_CENTER);
		
		messageLabel = new Label();
		messageLabel.addStyleName(Styles.error.toString());
		errorDetailLabel = new HtmlSimplifiedLabel();
		errorDetailLabel.addStyleName(Styles.emphasized.toString());
		errorDetailLabel.setVisible(false);
		ret.addComponents(messageLabel, errorDetailLabel);

		main = ret;
	}

	@Override
	public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public void triggerAuthentication()
	{
		startLogin();
	}

	@Override
	public void cancelAuthentication()
	{
		breakLogin(false);
	}

	@Override
	public String getLabel()
	{
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(idpKey);
		return providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, msg.getLocale());
	}

	@Override
	public Resource getImage()
	{
		OAuthClientProperties clientProperties = credentialExchange.getSettings();
		CustomProviderProperties providerProps = clientProperties.getProvider(idpKey);
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
		//nop
	}
	
	private void showError(String message)
	{
		if (message == null)
		{
			messageLabel.setValue("");
			showErrorDetail(null);
			return;
		}
		messageLabel.setValue(message);
	}

	private void showErrorDetail(String msgKey, Object... args)
	{
		if (msgKey == null)
		{
			errorDetailLabel.setVisible(false);
			errorDetailLabel.setValue("");
			return;
		}
		errorDetailLabel.setVisible(true);
		errorDetailLabel.setValue(msg.getMessage(msgKey, args));
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
	
	private void breakLogin(boolean invokeCancel)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
			contextManagement.removeAuthnContext(context.getRelayState());
		}
		if (invokeCancel)
			this.callback.cancelAuthentication();
	}
	
	private void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			NotificationPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("OAuth2Retrieval.loginInProgressError"));
			return;
		}
		URI requestURI = Page.getCurrent().getLocation();
		String servletPath = requestURI.getPath();
		String query = requestURI.getQuery() == null ? "" : "?" + requestURI.getQuery();
		String currentRelativeURI = servletPath + query;
		try
		{
			context = credentialExchange.createRequest(idpKey);
			context.setReturnUrl(currentRelativeURI);
			session.setAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT, context);
			context.setSandboxCallback(sandboxCallback);
		} catch (Exception e)
		{
			NotificationPopup.showError(msg, msg.getMessage("OAuth2Retrieval.configurationError"), e);
			log.error("Can not create OAuth2 request", e);
			breakLogin(true);
			return;
		}
		Page.getCurrent().open(servletPath + "?" + redirectParam, null);
	}

	
	/**
	 * Called when a OAuth authorization code response is received.
	 * @param authnContext
	 */
	private void onAuthzAnswer(OAuthContext authnContext)
	{
		log.debug("RetrievalUI received OAuth response");
		AuthenticationResult authnResult;
		showError(null);
		
		String reason = null;
		AuthenticationException savedException = null;
		try
		{
			authnResult = credentialExchange.verifyOAuthAuthzResponse(authnContext);
		} catch (AuthenticationException e)
		{
			savedException = e;
			reason = NotificationPopup.getHumanMessage(e, "<br>");
			authnResult = e.getResult();
		} catch (Exception e)
		{
			log.error("Runtime error during OAuth2 response processing or principal mapping", e);
			authnResult = new AuthenticationResult(Status.deny, null);
		}
		CustomProviderProperties providerProps = credentialExchange.getSettings().getProvider(
				authnContext.getProviderConfigKey()); 
		String regFormForUnknown = providerProps.getValue(CustomProviderProperties.REGISTRATION_FORM);
		if (regFormForUnknown != null)
			authnResult.setFormForUnknownPrincipal(regFormForUnknown);
		
		if (authnResult.getStatus() == Status.success || 
				authnResult.getStatus() == Status.unknownRemotePrincipal)
		{
			showError(null);
			breakLogin(false);
		} else
		{
			if (savedException != null)
				log.debug("OAuth2 authorization code verification or processing failed", 
						savedException);
			else
				log.debug("OAuth2 authorization code verification or processing failed");
			if (reason != null)
				showErrorDetail("OAuth2Retrieval.authnFailedDetailInfo", reason);
			showError(msg.getMessage("OAuth2Retrieval.authnFailedError"));
			breakLogin(false);
		}

		callback.setAuthenticationResult(authnResult);
	}

	/**
	 * {@inheritDoc}
	 */
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
	public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback) 
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
}
