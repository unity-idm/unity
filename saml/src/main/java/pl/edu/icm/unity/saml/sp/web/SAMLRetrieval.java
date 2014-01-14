/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;

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
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.ProgressIndicator;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.themes.Reindeer;

import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.web.SAMLSPRetrievalProperties.Binding;
import pl.edu.icm.unity.server.JettyServer;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;

/**
 * UI, Vaadin part of the SAML authn. 
 * @see SAMLRetrievalFactory
 * 
 * @author K. Benedyczak
 */
public class SAMLRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	//private Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrieval.class);
	public static final String REMOTE_AUTHN_CONTEXT = SAMLRetrieval.class.getName() + ".REMOTE_AUTHN_CONTEXT";
	
	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private SAMLSPRetrievalProperties samlProperties;
	private URL baseAddress;

	public SAMLRetrieval(UnityMessageSource msg, JettyServer jettyServer)
	{
		this.msg = msg;
		this.baseAddress = jettyServer.getAdvertisedAddress();
	}

	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		StringWriter sbw = new StringWriter();
		try
		{
			samlProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize SAML credential retrieval configuration", e);
		}
		return sbw.toString();	
	}

	@Override
	public void setSerializedConfiguration(String source)
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			samlProperties = new SAMLSPRetrievalProperties(properties);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML credential retrieval", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML credential retrieval(?)", e);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (SAMLExchange) e;
	}


	@Override
	public VaadinAuthenticationUI createUIInstance()
	{
		return new SAMLRetrievalUI();
	}


	private class SAMLRetrievalUI implements VaadinAuthenticationUI
	{	
		private Button loginButton;
		private Button cancelButton;
		private ProgressIndicator progress;
		
		
		public SAMLRetrievalUI()
		{
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
			
			Set<String> idps = samlProperties.getStructuredListKeys(SAMLSPRetrievalProperties.IDP_PREFIX);
			if (idps.size() > 1)
			{
				OptionGroup idpChooser = new OptionGroup(msg.getMessage("WebSAMLRetrieval.selectIdp"));
				idpChooser.setImmediate(true);
				for (String idpKey: idps)
				{
					String name = samlProperties.getValue(idpKey+SAMLSPRetrievalProperties.IDP_NAME);
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
						String name = samlProperties.getValue(key+SAMLSPRetrievalProperties.IDP_NAME);
						loginButton.setCaption(msg.getMessage("WebSAMLRetrieval.loginAt", name));
					}
				});
				ret.addComponent(idpChooser);
			}
			
			String key = idps.iterator().next();
			loginButton.setData(key);
			String name = samlProperties.getValue(key+SAMLSPRetrievalProperties.IDP_NAME);
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
			
			ret.addComponents(loginButton, hl);
			
			switchInProgress(checkInProgress());
			
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
		
		private void switchInProgress(boolean inProgress)
		{
			progress.setVisible(inProgress);
			cancelButton.setVisible(inProgress);
			loginButton.setEnabled(!inProgress);
		}
		
		private boolean checkInProgress()
		{
			WrappedSession session = VaadinSession.getCurrent().getSession();
			return session.getAttribute(REMOTE_AUTHN_CONTEXT) != null;
		}
		
		private void breakLogin()
		{
			WrappedSession session = VaadinSession.getCurrent().getSession();
			session.removeAttribute(REMOTE_AUTHN_CONTEXT);
			switchInProgress(false);
		}
		
		private void startLogin(String idpKey)
		{
			WrappedSession session = VaadinSession.getCurrent().getSession();
			if (session.getAttribute(REMOTE_AUTHN_CONTEXT) != null)
			{
				switchInProgress(true);
				ErrorPopup.showError(msg, msg.getMessage("error"), 
						msg.getMessage("WebSAMLRetrieval.loginInProgressError"));
				return;
			}
			RemoteAuthnContext context = new RemoteAuthnContext();
			session.setAttribute(REMOTE_AUTHN_CONTEXT, context);
			switchInProgress(true);
			
			
			String identityProviderURL = samlProperties.getValue(
					idpKey + SAMLSPRetrievalProperties.IDP_ADDRESS);
			String servletPath = VaadinServlet.getCurrent().getServletContext().getContextPath() + 
					VaadinServletService.getCurrentServletRequest().getServletPath();
			String responseUrl = baseAddress + servletPath + ResponseConsumerRequestHandler.PATH;
			AuthnRequestDocument request = credentialExchange.createSAMLRequest(
					identityProviderURL, responseUrl);
			context.setRequest(request.xmlText());
			context.setIdpUrl(identityProviderURL);
			context.setBinding(samlProperties.getEnumValue(idpKey + SAMLSPRetrievalProperties.IDP_BINDING, 
					Binding.class));
			
			Page.getCurrent().open(servletPath + RedirectRequestHandler.PATH, null);
		}
		
		@Override
		public void setUsernameCallback(UsernameProvider usernameCallback)
		{
		}

		@Override
		public AuthenticationResult getAuthenticationResult()
		{
			//TODO
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
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel()
		{
			return samlProperties.getValue(SAMLSPRetrievalProperties.DISPLAY_NAME);
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
}










