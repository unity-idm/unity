/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.UIBgThread;
import pl.edu.icm.unity.webui.common.idpselector.IdPsSpecification;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinServlet;
import com.vaadin.server.VaadinServletService;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

/**
 * The UI part of the remote SAML authn. Shows widget allowing to choose IdP (if more then one is configured)
 * starts the authN and awaits for answer in the context. When it is there, the validator is contacted for verification.
 * It is also possible to cancel the authentication which is in progress.
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthenticationUI
{	
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrievalUI.class);
	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private AuthenticationResultCallback callback;
	
	private IdpSelectorComponent idpSelector;
	private Label messageLabel;
	private Label errorDetailLabel;
	private ResponseWaitingThread waitingThread;
	private SamlContextManagement samlContextManagement;
	
	
	public SAMLRetrievalUI(UnityMessageSource msg, SAMLExchange credentialExchange, 
			SamlContextManagement samlContextManagement)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
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
		
		final SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		
		Label title = new Label(samlProperties.getValue(SAMLSPProperties.DISPLAY_NAME));
		title.addStyleName(Reindeer.LABEL_H2);
		ret.addComponent(title);

		Label subtitle = new Label(msg.getMessage("WebSAMLRetrieval.selectIdp"));
		ret.addComponent(subtitle);
		final Set<String> idps = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		int perRow = samlProperties.getIntValue(SAMLSPProperties.PROVIDERS_IN_ROW);
		idpSelector = new IdpSelectorComponent(msg, perRow, new IdPsSpecification()
		{
			@Override
			public Collection<String> getIdpKeys()
			{
				return idps;
			}
			
			@Override
			public String getIdPName(String key, Locale locale)
			{
				return samlProperties.getLocalizedName(key, msg.getLocale());
			}
			
			@Override
			public String getIdPLogoUri(String key, Locale locale)
			{
				return samlProperties.getLocalizedValue(key + SAMLSPProperties.IDP_LOGO, 
						msg.getLocale());
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

		checkInProgress();
		
		return ret;
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
	
	private synchronized void switchInProgress(RemoteAuthnContext context)
	{
		boolean inProgress = context != null;
		if (inProgress)
		{
			if (waitingThread != null)
				waitingThread.forceStop();
			waitingThread = new ResponseWaitingThread(context);
			new Thread(waitingThread).start();
		} else
		{
			if (waitingThread != null)
				waitingThread.forceStop();
		}
	}
	
	private void checkInProgress()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		switchInProgress(context);
	}
	
	private void breakLogin(boolean invokeCancel)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
			samlContextManagement.removeAuthnContext(context.getRelayState());
		}
		switchInProgress(null);
		if (invokeCancel)
			this.callback.cancelAuthentication();
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
	
	private void startLogin(String idpKey)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			switchInProgress(context);
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("WebSAMLRetrieval.loginInProgressError"));
			return;
		}
		context = new RemoteAuthnContext();
		session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
		samlContextManagement.addAuthnContext(context);
		switchInProgress(context);
		
		SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		AuthnRequestDocument request;
		try
		{
			request = credentialExchange.createSAMLRequest(idpKey);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("WebSAMLRetrieval.configurationError"), e);
			log.error("Can not create SAML request", e);
			breakLogin(true);
			return;
		}
		Binding requestBinding = samlProperties.getEnumValue(idpKey + SAMLSPProperties.IDP_BINDING, 
				Binding.class);
		String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
				VaadinServletService.getCurrentServletRequest().getServletPath();
		String identityProviderURL = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		String groupAttribute = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_GROUP_MEMBERSHIP_ATTRIBUTE);
		String registrationFormForUnknown = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_REGISTRATION_FORM);
		String translationProfile = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_TRANSLATION_PROFILE);
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), 
				requestBinding, identityProviderURL, servletPath, groupAttribute, 
				registrationFormForUnknown, translationProfile);
		
		
		Page.getCurrent().open(servletPath + RedirectRequestHandler.PATH, null);
	}

	/**
	 * Called when a SAML response is received. This method is not called from the UI thread!
	 * @param samleResponse
	 */
	private void onSamlAnswer(RemoteAuthnContext authnContext)
	{
		VaadinSession session = VaadinSession.getCurrent();
		session.lock();
		AuthenticationResult authnResult;
		showError(null);
		try
		{
			String reason = null;
			Exception savedException = null;
			try
			{
				authnResult = credentialExchange.verifySAMLResponse(authnContext);
			} catch (AuthenticationException e)
			{
				savedException = e;
				reason = ErrorPopup.getHumanMessage(e, "<br>");
				authnResult = e.getResult();
			} catch (Exception e)
			{
				log.error("Runtime error during SAML response processing or principal mapping", e);
				authnResult = new AuthenticationResult(Status.deny, null);
			}

			if (authnResult.getStatus() == Status.success)
			{
				showError(null);
				breakLogin(false);
			} else if (authnResult.getStatus() == Status.unknownRemotePrincipal && 
					authnContext.getRegistrationFormForUnknown() != null) 
			{
				log.debug("There is a registration form to show for the unknown user: " + 
						authnContext.getRegistrationFormForUnknown());
				authnResult.setFormForUnknownPrincipal(authnContext.getRegistrationFormForUnknown());
				showError(null);
				breakLogin(false);
			} else
			{
				if (savedException != null)
					log.warn("SAML response verification or processing failed", savedException);
				else
					log.warn("SAML response verification or processing failed");
				if (reason != null)
					showErrorDetail(msg.getMessage("WebSAMLRetrieval.authnFailedDetailInfo", reason));
				showError(msg.getMessage("WebSAMLRetrieval.authnFailedError"));
				breakLogin(false);
			}

		} finally
		{
			session.unlock();
		}
		callback.setAuthenticationResult(authnResult);
	}
	
	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
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

	/**
	 * Waits for the SAML answer which should appear in the session's {@link RemoteAuthnContext}. 
	 * @author K. Benedyczak
	 */
	private class ResponseWaitingThread extends UIBgThread
	{
		private RemoteAuthnContext context;
		private boolean stop = false;
		
		public ResponseWaitingThread(RemoteAuthnContext context)
		{
			this.context = context;
		}

		public void safeRun()
		{
			while (!isStopped())
			{
				if (context.getResponse() == null)
				{
					try
					{
						Thread.sleep(100);
					} catch (InterruptedException e)
					{
						//ok
					}
				} else
				{
					onSamlAnswer(context);
					break;
				}
			}
		}
		
		public synchronized void forceStop()
		{
			stop = true;
		}
		
		private synchronized boolean isStopped()
		{
			return stop;
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLabel()
	{	
		return credentialExchange.getSamlValidatorSettings().getValue(SAMLSPProperties.DISPLAY_NAME);
	}

	/**
	 * {@inheritDoc}
	 */
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
}