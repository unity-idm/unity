/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasEnabled;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.VaadinServletService;
import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.util.configuration.ConfigurationException;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;
import io.imunity.vaadin.auth.VaadinAuthentication;

import javax.servlet.http.HttpServletRequest;
import java.io.StringReader;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

/**
 * Retrieves the authenticated user from the TLS. The login happens on the HTTP connection level 
 * and so the component is not interactive.
 */
@org.springframework.stereotype.Component("WebTLSRetrieval")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TLSRetrieval extends AbstractCredentialRetrieval<CertificateExchange> implements VaadinAuthentication
{
	public static final String NAME = "vaadin-certificate";
	public static final String DESC = "WebTLSRetrievalFactory.desc";
	
	private final MessageSource msg;
	private I18nString name;
	private String registrationFormForUnknown;
	private boolean enableAssociation;
	private String configuration;
	
	@Autowired
	public TLSRetrieval(MessageSource msg)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		return configuration;
	}

	@Override
	public void setSerializedConfiguration(String configuration)
	{
		this.configuration = configuration;
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(configuration));
			TLSRetrievalProperties config = new TLSRetrievalProperties(properties);
			name = config.getLocalizedString(msg, TLSRetrievalProperties.NAME);
			if (name.isEmpty())
				name = new I18nString("WebTLSRetrieval.title", msg);
			registrationFormForUnknown = config.getValue(
					TLSRetrievalProperties.REGISTRATION_FORM_FOR_UNKNOWN);
			enableAssociation = config.getBooleanValue(TLSRetrievalProperties.ENABLE_ASSOCIATION);
			
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based TLS retrieval can not be parsed", e);
		}
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authenticatorContext)
	{
		return Collections.<VaadinAuthenticationUI>singleton(new TLSRetrievalUI());
	}

	@Override
	public boolean supportsGrid()
	{
		return false; //TODO this component can support grid
	}

	@Override
	public boolean isMultiOption()
	{
		return false;
	}
	
	private AuthenticationRetrievalContext getContext()
	{
		return AuthenticationRetrievalContext.builder().withSupportOnlySecondFactorReseting(false).build();
	}
	
	public static X509Certificate[] getTLSCertificate()
	{
		HttpServletRequest request = VaadinServletService.getCurrentServletRequest();
		if (request == null)
			return null;
		return (X509Certificate[]) request.getAttribute(
				"javax.servlet.request.X509Certificate");
	} 
	
	private class TLSRetrievalUI implements VaadinAuthenticationUI
	{	
		private final Component component = new TLSAuthnComponent();
		private AuthenticationCallback callback;
		
		public TLSRetrievalUI()
		{
		}

		@Override
		public Component getComponent()
		{
			return component;
		}

		@Override
		public void setAuthenticationCallback(AuthenticationCallback callback)
		{
			this.callback = callback;
		}

		private AuthenticationResult getAuthenticationResult()
		{
			X509Certificate[] clientCert = getTLSCertificate();

			if (clientCert == null)
				return LocalAuthenticationResult.notApplicable();

			return credentialExchange.checkCertificate(clientCert,
					registrationFormForUnknown, enableAssociation, callback.getTriggeringContext());
		}

		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

		@Override
		public Image getImage()
		{
			Image image = new Image("../unitygw/img/other/certificate.png", "");
			image.getStyle().set("max-height", "1.65rem");
			image.getStyle().set("padding-top", "0.25em");
			return image;
		}

		private class TLSAuthnComponent extends VerticalLayout
		{
			public TLSAuthnComponent()
			{
				setMargin(false);
				setPadding(false);
				X509Certificate[] clientCert = getTLSCertificate();
				String info = clientCert == null ? "" : msg.getMessage("WebTLSRetrieval.certInfo", 
						X500NameUtils.getReadableForm(clientCert[0].getSubjectX500Principal()));
				Button authenticateButton = new Button(msg.getMessage("WebTLSRetrieval.signInButton"));
				authenticateButton.addClickListener(event -> triggerAuthentication());
				authenticateButton.setIcon(getImage());
				authenticateButton.addClassName("u-x509SignInButton");
				authenticateButton.setWidthFull();
				authenticateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
				authenticateButton.getElement().setProperty("title", info);
				add(authenticateButton);
			}
		}
		
		private void triggerAuthentication()
		{
			callback.onStartedAuthentication();
			AuthenticationResult authenticationResult = getAuthenticationResult();
			if (authenticationResult.getStatus() == Status.success)
				((HasEnabled)component).setEnabled(false);
			
			callback.onCompletedAuthentication(authenticationResult, getContext());
		}
		
		@Override
		public boolean isAvailable()
		{
			return getTLSCertificate() != null;
		}
		
		@Override
		public void clear()
		{
			//nop
		}

		/**
		 * Simple: there is only one authN option in this authenticator so we can return any constant id. 
		 */
		@Override
		public String getId()
		{
			return "certificate";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
		}
		

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}
	}
	
	
	@org.springframework.stereotype.Component("WebTLSRetrievalFactory")
	public static class Factory extends AbstractCredentialRetrievalFactory<TLSRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<TLSRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, CertificateExchange.ID);
		}
	}
}


