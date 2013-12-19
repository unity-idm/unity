/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.util.configuration.ConfigurationException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.CredentialExchange;
import pl.edu.icm.unity.server.authn.CredentialRetrieval;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.CertificateExchange;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;

/**
 * Retrieves the authenticated user from the TLS. The login happens on the HTTP connection level 
 * and so the component is not interactive.
 * 
 * @author K. Benedyczak
 */
public class TLSRetrieval implements CredentialRetrieval, VaadinAuthentication
{
	private CertificateExchange credentialExchange;
	private UnityMessageSource msg;
	private String name;
	
	public TLSRetrieval(UnityMessageSource msg)
	{
		this.msg = msg;
	}
	
	@Override
	public String getBindingName()
	{
		return VaadinAuthentication.NAME;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.put("name", name);
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize web-based TLS retrieval configuration to JSON", e);
		}
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
		try
		{
			JsonNode root = Constants.MAPPER.readTree(json);
			name = root.get("name").asText();
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based TLS retrieval can not be parsed", e);
		}
	}

	@Override
	public void setCredentialExchange(CredentialExchange e)
	{
		this.credentialExchange = (CertificateExchange) e;
	}

	@Override
	public VaadinAuthenticationUI createUIInstance()
	{
		return new TLSRetrievalUI();
	}

	
	private class TLSRetrievalUI implements VaadinAuthenticationUI
	{
		private TLSAuthnComponent component;

		@Override
		public boolean needsCommonUsernameComponent()
		{
			return false;
		}

		@Override
		public Component getComponent()
		{
			component = new TLSAuthnComponent();
			return component;
		}

		@Override
		public void setUsernameCallback(UsernameProvider usernameCallback)
		{
		}

		@Override
		public AuthenticationResult getAuthenticationResult()
		{
			X509Certificate[] clientCert = getTLSCertificate();

			if (clientCert == null)
			{
				return new AuthenticationResult(Status.notApplicable, null);
			}
			try
			{
				AuthenticationResult authenticationResult = credentialExchange.checkCertificate(clientCert);
				component.setError(authenticationResult.getStatus() != Status.success);
				return authenticationResult;
			} catch (Exception e)
			{
				component.setError(true);
				return new AuthenticationResult(Status.deny, null);
			}
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel()
		{
			return name;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Resource getImage()
		{
			return null;
		}


		private X509Certificate[] getTLSCertificate()
		{
			HttpServletRequest request = VaadinServletService.getCurrentServletRequest();
			if (request == null)
				return null;
			return (X509Certificate[]) request.getAttribute(
					"javax.servlet.request.X509Certificate");
		}

		@SuppressWarnings("serial")
		private class TLSAuthnComponent extends VerticalLayout
		{
			private Label info;

			public TLSAuthnComponent()
			{
				String label = name.trim().equals("") ? msg.getMessage("WebTLSRetrieval.title") : name;
				Label title = new Label(label);
				title.addStyleName(Reindeer.LABEL_H2);
				addComponent(title);
				info = new Label();
				addComponent(info);
				X509Certificate[] clientCert = getTLSCertificate();
				if (clientCert == null)
				{
					info.setValue(msg.getMessage("WebTLSRetrieval.noCert"));
				} else
				{
					info.setValue(msg.getMessage("WebTLSRetrieval.certInfo", 
							X500NameUtils.getReadableForm(clientCert[0].getSubjectX500Principal())));
				}
			}

			public void setError(boolean how)
			{
				info.setComponentError(how ? new UserError(
						msg.getMessage("WebTLSRetrieval.unknownUser")) : null);
			}
		}
	}	
}


