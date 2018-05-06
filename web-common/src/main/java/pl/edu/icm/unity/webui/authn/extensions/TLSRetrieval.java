/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.net.MalformedURLException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServletService;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import eu.emi.security.authn.x509.impl.X500NameUtils;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.credential.cert.CertificateExchange;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;

/**
 * Retrieves the authenticated user from the TLS. The login happens on the HTTP connection level 
 * and so the component is not interactive.
 * 
 * @author K. Benedyczak
 */
@org.springframework.stereotype.Component("WebTLSRetrieval")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TLSRetrieval extends AbstractCredentialRetrieval<CertificateExchange> implements VaadinAuthentication
{
	public static final String NAME = "web-certificate";
	public static final String DESC = "WebTLSRetrievalFactory.desc";
	
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, TLSRetrieval.class);
	private UnityMessageSource msg;
	private I18nString name;
	private String logoURL;
	
	@Autowired
	public TLSRetrieval(UnityMessageSource msg)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
	}
	
	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("i18nName", I18nStringJsonUtil.toJson(name));
		if (logoURL != null)
			root.put("logoURL", logoURL);
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
			name = I18nStringJsonUtil.fromJson(root.get("i18nName"), root.get("name"));
			if (name.isEmpty())
				name = new I18nString("WebTLSRetrieval.title", msg);
			JsonNode logoNode = root.get("logoURL");
			if (logoNode != null && !logoNode.isNull())
				logoURL = logoNode.asText();
			if (logoURL != null && !logoURL.isEmpty())
				ImageUtils.getLogoResource(logoURL);
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based TLS retrieval can not be parsed", e);
		}
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance()
	{
		return Collections.<VaadinAuthenticationUI>singleton(new TLSRetrievalUI());
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
		private TLSAuthnComponent component = new TLSAuthnComponent();
		private AuthenticationResultCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		
		@Override
		public Component getComponent()
		{
			return component;
		}

		@Override
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
		{
			this.callback = callback;
		}

		@Override
		public void triggerAuthentication()
		{
			callback.setAuthenticationResult(getAuthenticationResult());
		}
		
		private AuthenticationResult getAuthenticationResult()
		{
			X509Certificate[] clientCert = getTLSCertificate();

			if (clientCert == null)
				return new AuthenticationResult(Status.notApplicable, null);

			AuthenticationResult authenticationResult = credentialExchange.checkCertificate(
					clientCert, sandboxCallback);
			component.setError(authenticationResult.getStatus() != Status.success);
			return authenticationResult;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Resource getImage()
		{
			if (logoURL == null)
				return null;
			if ("".equals(logoURL))
				return Images.certificate.getResource();
			else
			{
				try
				{
					return ImageUtils.getLogoResource(logoURL);
				} catch (MalformedURLException e)
				{
					log.error("Can't load logo", e);
					return null;
				}
			}

		}

		@SuppressWarnings("serial")
		private class TLSAuthnComponent extends VerticalLayout
		{
			private Label info;

			public TLSAuthnComponent()
			{
				setMargin(false);
				setSpacing(false);
				Label title = new Label(name.getValue(msg));
				title.addStyleName(Styles.vLabelLarge.toString());
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

		@Override
		public void cancelAuthentication()
		{
			//nop
		}

		@Override
		public void clear()
		{
			//nop
		}

		@Override
		public void refresh(VaadinRequest request) 
		{
			//nop
		}

		@Override
		public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback) 
		{
			sandboxCallback = callback;
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
			super(NAME, DESC, VaadinAuthentication.NAME, factory, CertificateExchange.class);
		}
	}
}


