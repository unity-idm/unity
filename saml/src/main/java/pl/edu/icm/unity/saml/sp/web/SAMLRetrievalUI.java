/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.net.URL;
import java.util.Collection;
import java.util.Set;

import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties.Binding;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.UsernameProvider;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
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
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

/**
 * The UI part of the remote SAML authn. Shows widget allowing to choose IdP (if more then one is configured)
 * starts the authN and awaits for answer in the context. When it is there, the validator is contacted for verification.
 * It is also possible to cancel the authentication which is in progress.
 * FIXME - review synchro
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthenticationUI
{	
	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private URL baseAddress;
	
	private Button loginButton;
	private Button cancelButton;
	private ProgressIndicator progress;
	private Label error;
	private ResponseWaitingThread waitingThread;
	private AuthenticationResult authnResult = null;
	
	public SAMLRetrievalUI(UnityMessageSource msg, SAMLExchange credentialExchange, URL baseAddress)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.baseAddress = baseAddress;
	}

	@Override
	public boolean needsCommonUsernameComponent()
	{
		return false;
	}

	@Override
	public Component getComponent()
	{
		installRequestHandlers();
		
		VerticalLayout ret = new VerticalLayout();
		ret.setSpacing(true);
		loginButton = new Button();
		final SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
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
					String key = (String) event.getProperty().getValue();
					loginButton.setData(key);
					String name = samlProperties.getValue(key+SAMLSPProperties.IDP_NAME);
					loginButton.setCaption(msg.getMessage("WebSAMLRetrieval.loginAt", name));
				}
			});
			ret.addComponent(idpChooser);
		}
		
		String key = idps.iterator().next();
		loginButton.setData(key);
		String name = samlProperties.getValue(key+SAMLSPProperties.IDP_NAME);
		loginButton.setCaption(msg.getMessage("WebSAMLRetrieval.loginAt", name));
		
		loginButton.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				startLogin((String) loginButton.getData());
			}
		});
		HorizontalLayout hl = new HorizontalLayout();
		hl.setSpacing(true);
		
		progress = new ProgressIndicator();
		progress.setIndeterminate(true);
		progress.setCaption(msg.getMessage("WebSAMLRetrieval.authnInProgress"));
		cancelButton = new Button(msg.getMessage("cancel"));
		cancelButton.addStyleName(Reindeer.BUTTON_SMALL);
		cancelButton.addClickListener(new Button.ClickListener()
		{
			@Override
			public void buttonClick(ClickEvent event)
			{
				breakLogin();
			}
		});
		hl.addComponents(progress, cancelButton);
		
		error = new Label();
		error.addStyleName(Styles.error.toString());
		error.setVisible(false);
		
		ret.addComponents(loginButton, hl, error);

		checkInProgress();
		
		return ret;
	}

	private void installRequestHandlers()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		boolean redirectInstalled = false;
		boolean responseInstalled = false;
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof RedirectRequestHandler)
				redirectInstalled = true;
			if (rh instanceof ResponseConsumerRequestHandler)
				responseInstalled = true;
		}
		if (!redirectInstalled)
			session.addRequestHandler(new RedirectRequestHandler());
		if (!responseInstalled)
			session.addRequestHandler(new ResponseConsumerRequestHandler());
	}
	
	private synchronized void switchInProgress(RemoteAuthnContext context)
	{
		boolean inProgress = context != null;
		progress.setVisible(inProgress);
		cancelButton.setVisible(inProgress);
		loginButton.setEnabled(!inProgress);
		if (inProgress)
		{
			if (waitingThread != null)
				waitingThread.forceStop();
			waitingThread = new ResponseWaitingThread(context);
			waitingThread.start();
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
	
	private void breakLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		session.removeAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		switchInProgress(null);
		authnResult = null;
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
		authnResult = null;
		context = new RemoteAuthnContext();
		session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
		switchInProgress(context);
		
		SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		String identityProviderURL = samlProperties.getValue(
				idpKey + SAMLSPProperties.IDP_ADDRESS);
		String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
				VaadinServletService.getCurrentServletRequest().getServletPath();
		String responseUrl = baseAddress + servletPath + ResponseConsumerRequestHandler.PATH;
		AuthnRequestDocument request = credentialExchange.createSAMLRequest(
				identityProviderURL, responseUrl);
		Binding requestBinding = samlProperties.getEnumValue(idpKey + SAMLSPProperties.IDP_BINDING, 
				Binding.class);
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), responseUrl,
				requestBinding, identityProviderURL);
		
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
		try
		{
			breakLogin();
			try
			{
				authnResult = credentialExchange.verifySAMLResponse(authnContext);
			} catch (Exception e)
			{
				authnResult = new AuthenticationResult(Status.deny, null);
			}
			if (authnResult.getStatus() == Status.deny)
			{
				error.setValue(msg.getMessage("WebSAMLRetrieval.authnFailedError"));
				error.setVisible(true);
			} else
				error.setVisible(false);
			
		} finally
		{
			session.unlock();
		}
	}
	
	@Override
	public void setUsernameCallback(UsernameProvider usernameCallback)
	{
	}

	@Override
	public AuthenticationResult getAuthenticationResult()
	{
		if (authnResult != null)
			return authnResult;
		error.setValue(msg.getMessage("WebSAMLRetrieval.notYetLoggedError"));
		error.setVisible(true);
		return new AuthenticationResult(Status.deny, null);
		/*
		String username = usernameProvider.getUsername();
		String password = passwordField.getValue();
		if (username.equals("") && password.equals(""))
		{
			passwordField.setComponentError(new UserError(
					msg.getMessage("WebPasswordRetrieval.noPassword")));
			return new AuthenticationResult(Status.notApplicable, null);
		}
		try
		{
			AuthenticationResult authenticationResult = credentialExchange.checkPassword(username, password);
			if (authenticationResult.getStatus() == Status.success)
				passwordField.setComponentError(null);
			else if (authenticationResult.getStatus() == Status.unknownRemotePrincipal && 
					registrationFormForUnknown != null) 
			{
				authenticationResult.setFormForUnknownPrincipal(registrationFormForUnknown);
				passwordField.setValue("");
			} else
			{
				passwordField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.wrongPassword")));
				passwordField.setValue("");
			}
			return authenticationResult;
		} catch (Exception e)
		{
			if (!(e instanceof IllegalCredentialException))
				log.warn("Password verificator has thrown an exception", e);
			passwordField.setComponentError(new UserError(
					msg.getMessage("WebPasswordRetrieval.wrongPassword")));
			passwordField.setValue("");
			return new AuthenticationResult(Status.deny, null);
		}
		*/
	}


	/**
	 * Waits for the SAML answer which should appear in the session's {@link RemoteAuthnContext}. 
	 * @author K. Benedyczak
	 */
	private class ResponseWaitingThread extends Thread
	{
		private RemoteAuthnContext context;
		private boolean stop = false;
		
		public ResponseWaitingThread(RemoteAuthnContext context)
		{
			this.context = context;
		}

		public void run()
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
}