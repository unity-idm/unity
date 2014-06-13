/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.oauth.client.web;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;

import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import pl.edu.icm.unity.oauth.client.OAuthContext;
import pl.edu.icm.unity.oauth.client.OAuthContextsManagement;
import pl.edu.icm.unity.oauth.client.OAuthExchange;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties;
import pl.edu.icm.unity.oauth.client.config.OAuthClientProperties;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.idpselector.IdPsSpecification;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;

/**
 * UI part of OAuth retrieval. Shows available providers, redirects to the chosen one.
 * @author K. Benedyczak
 */
public class OAuth2RetrievalUI implements VaadinAuthenticationUI
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_OAUTH, OAuth2RetrievalUI.class);
	public static final String CHOSEN_IDP_COOKIE = "lastOAuthIdP";
	
	private UnityMessageSource msg;
	private OAuthExchange credentialExchange;
	private OAuthContextsManagement contextManagement;
	
	private AuthenticationResultCallback callback;

	private IdpSelectorComponent idpSelector;
	private Label messageLabel;
	private Label errorDetailLabel;
	
	public OAuth2RetrievalUI(UnityMessageSource msg, OAuthExchange credentialExchange,
			OAuthContextsManagement contextManagement, ExecutorsService executorsService)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.contextManagement = contextManagement;
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return false;
	}

	@Override
	public Component getComponent()
	{
		installRequestHandler();

		final OAuthClientProperties clientProperties = credentialExchange.getSettings();
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		
		Label title = new Label(clientProperties.getValue(OAuthClientProperties.DISPLAY_NAME));
		title.addStyleName(Reindeer.LABEL_H2);
		ret.addComponent(title);
		
		Label subtitle = new Label(msg.getMessage("OAuth2Retrieval.selectProvider"));
		ret.addComponent(subtitle);
		
		final Set<String> idps = clientProperties.getStructuredListKeys(OAuthClientProperties.PROVIDERS);
		int perRow = clientProperties.getIntValue(OAuthClientProperties.PROVIDERS_IN_ROW);
		ScaleMode scaleMode = clientProperties.getEnumValue(OAuthClientProperties.ICON_SCALE, ScaleMode.class); 
		idpSelector = new IdpSelectorComponent(msg, perRow, scaleMode, CHOSEN_IDP_COOKIE, new IdPsSpecification()
		{
			@Override
			public Collection<String> getIdpKeys()
			{
				return idps;
			}
			
			@Override
			public String getIdPName(String key, Locale locale)
			{
				CustomProviderProperties providerProps = clientProperties.getProvider(key);
				return providerProps.getLocalizedValue(CustomProviderProperties.PROVIDER_NAME, 
						locale);
			}
			
			@Override
			public String getIdPLogoUri(String key, Locale locale)
			{
				CustomProviderProperties providerProps = clientProperties.getProvider(key);
				return providerProps.getLocalizedValue(CustomProviderProperties.ICON_URL, 
						locale);
			}
		});
		ret.addComponent(idpSelector);
		
		messageLabel = new Label();
		messageLabel.setContentMode(ContentMode.HTML);
		messageLabel.addStyleName(Styles.error.toString());
		errorDetailLabel = new Label();
		errorDetailLabel.setContentMode(ContentMode.HTML);
		errorDetailLabel.addStyleName(Styles.italic.toString());
		errorDetailLabel.setVisible(false);
		ret.addComponents(messageLabel, errorDetailLabel);

		return ret;
	}

	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
		//nop
	}

	@Override
	public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public void triggerAuthentication()
	{
		startLogin(idpSelector.getSelectedProvider());
	}

	@Override
	public void cancelAuthentication()
	{
		breakLogin(false);
	}

	@Override
	public String getLabel()
	{
		return credentialExchange.getSettings().getValue(OAuthClientProperties.DISPLAY_NAME);
	}

	@Override
	public Resource getImage()
	{
		return null;
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

	private void showErrorDetail(String message)
	{
		if (message == null)
		{
			errorDetailLabel.setVisible(false);
			errorDetailLabel.setValue("");
			return;
		}
		errorDetailLabel.setVisible(true);
		errorDetailLabel.setValue(message);
	}
	
	private void installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		boolean redirectInstalled = false;
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof RedirectRequestHandler)
				redirectInstalled = true;
		}
		if (!redirectInstalled)
			session.addRequestHandler(new RedirectRequestHandler());
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
	
	private void startLogin(String providerKey)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		OAuthContext context = (OAuthContext) session.getAttribute(
				OAuth2Retrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("OAuth2Retrieval.loginInProgressError"));
			return;
		}
		
		try
		{
			context = credentialExchange.createRequest(providerKey);
			String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
					VaadinServletService.getCurrentServletRequest().getServletPath();
			context.setReturnUrl(servletPath);
			session.setAttribute(OAuth2Retrieval.REMOTE_AUTHN_CONTEXT, context);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("OAuth2Retrieval.configurationError"), e);
			log.error("Can not create OAuth2 request", e);
			breakLogin(true);
			return;
		}
		String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
				VaadinServletService.getCurrentServletRequest().getServletPath();
		
		IdpSelectorComponent.setLastIdpCookie(CHOSEN_IDP_COOKIE, context.getProviderConfigKey());
		Page.getCurrent().open(servletPath + RedirectRequestHandler.PATH, null);
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
		
		log.debug("RetrievalUI will validate OAuth response");
		String reason = null;
		Exception savedException = null;
		try
		{
			authnResult = credentialExchange.verifyOAuthAuthzResponse(authnContext);
		} catch (AuthenticationException e)
		{
			savedException = e;
			reason = ErrorPopup.getHumanMessage(e, "<br>");
			authnResult = e.getResult();
		} catch (Exception e)
		{
			log.error("Runtime error during OAuth2 response processing or principal mapping", e);
			authnResult = new AuthenticationResult(Status.deny, null);
		}
		CustomProviderProperties providerProps = credentialExchange.getSettings().getProvider(
				authnContext.getProviderConfigKey()); 
		String regFormForUnknown = providerProps.getValue(CustomProviderProperties.REGISTRATION_FORM);
		if (authnResult.getStatus() == Status.success)
		{
			showError(null);
			breakLogin(false);
		} else if (authnResult.getStatus() == Status.unknownRemotePrincipal && 
				regFormForUnknown != null) 
		{
			log.debug("There is a registration form to show for the unknown user: " + 
					regFormForUnknown);
			authnResult.setFormForUnknownPrincipal(regFormForUnknown);
			showError(null);
			breakLogin(false);
		} else
		{
			if (savedException != null)
				log.warn("OAuth2 authorization code verification or processing failed", 
						savedException);
			else
				log.warn("OAuth2 authorization code verification or processing failed");
			if (reason != null)
				showErrorDetail(msg.getMessage("OAuth2Retrieval.authnFailedDetailInfo", reason));
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

}
