/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLResponseConsumerServlet;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.InvocationContext;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.UIBgThread;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
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
import com.vaadin.ui.OptionGroup;
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
	private URL baseAddress;
	private String baseContext;
	private AuthenticationResultCallback callback;
	private String registrationFormForUnknown;
	
	private String selectedIdp;
	private Label messageLabel;
	private Label errorDetailLabel;
	private ResponseWaitingThread waitingThread;
	private SamlContextManagement samlContextManagement;
	
	
	public SAMLRetrievalUI(UnityMessageSource msg, SAMLExchange credentialExchange, URL baseAddress,
			String baseContext, SamlContextManagement samlContextManagement)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.baseAddress = baseAddress;
		this.baseContext = baseContext;
		this.samlContextManagement = samlContextManagement;
		this.registrationFormForUnknown = credentialExchange.getSamlValidatorSettings().getValue(
				SAMLSPProperties.REGISTRATION_FORM);
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
		
		Set<String> idps = samlProperties.getStructuredListKeys(SAMLSPProperties.IDP_PREFIX);
		if (idps.size() > 1)
		{
			OptionGroup idpChooser = new OptionGroup(msg.getMessage("WebSAMLRetrieval.selectIdp"));
			idpChooser.setImmediate(true);
			for (String idpKey: idps)
			{
				String name = samlProperties.getValue(idpKey+SAMLSPProperties.IDP_NAME);
				idpChooser.addItem(idpKey);
				idpChooser.setItemCaption(idpKey, name);
			}
			idpChooser.select(idps.iterator().next());
			idpChooser.setNullSelectionAllowed(false);
			idpChooser.addValueChangeListener(new ValueChangeListener()
			{
				@Override
				public void valueChange(ValueChangeEvent event)
				{
					selectedIdp = (String) event.getProperty().getValue();
				}
			});
			ret.addComponent(idpChooser);
		} else
		{
			String idpKey = idps.iterator().next();
			String name = samlProperties.getValue(idpKey+SAMLSPProperties.IDP_NAME);
			Label selectedIdp = new Label(msg.getMessage("WebSAMLRetrieval.selectedIdp", name));
			ret.addComponent(selectedIdp);
		}
		
		selectedIdp = idps.iterator().next();
		
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
			errorDetailLabel.setVisible(false);
			return;
		}
		messageLabel.setValue(message);
	}

	private void showErrorDetail(String message)
	{
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
		String identityProviderURL = samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		boolean sign = samlProperties.getBooleanValue(idpKey + SAMLSPProperties.IDP_SIGN_REQUEST);
		String responseUrl = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
		AuthnRequestDocument request;
		try
		{
			request = credentialExchange.createSAMLRequest(identityProviderURL, responseUrl, sign);
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
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), responseUrl,
				requestBinding, identityProviderURL, servletPath);
		
		
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
		try
		{
			try
			{
				authnResult = credentialExchange.verifySAMLResponse(authnContext);
			} catch (AuthenticationException e)
			{
				log.warn("SAML response verification or processing failed", e);
				String reason = ErrorPopup.getHumanMessage(e, "<br>");
				showErrorDetail(msg.getMessage("WebSAMLRetrieval.authnFailedDetailInfo", reason));
				authnResult = e.getResult();
			} catch (Exception e)
			{
				log.error("Runtime error during SAML response processing or principal mapping", e);
				authnResult = new AuthenticationResult(Status.deny, null);
			}

			if (authnResult.getStatus() == Status.success)
			{
				showError(null);
			} else if (authnResult.getStatus() == Status.unknownRemotePrincipal && 
					registrationFormForUnknown != null) 
			{
				authnResult.setFormForUnknownPrincipal(registrationFormForUnknown);
				showError(null);
			} else
			{
				showError(msg.getMessage("WebSAMLRetrieval.authnFailedError"));
			}

			
			breakLogin(false);
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
		startLogin(selectedIdp);
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
		private InvocationContext invocationContext;
		
		public ResponseWaitingThread(RemoteAuthnContext context)
		{
			this.context = context;
			this.invocationContext = InvocationContext.getCurrent();
		}

		public void safeRun()
		{
			InvocationContext.setCurrent(invocationContext);
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
}