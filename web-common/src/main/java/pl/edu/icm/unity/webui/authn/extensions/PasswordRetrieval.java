/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.server.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.stdext.credential.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.PasswordVerificatorFactory;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nDescribedObject;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.CredentialReset1Dialog;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.server.Resource;
import com.vaadin.server.UserError;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
public class PasswordRetrieval extends AbstractCredentialRetrieval<PasswordExchange> implements VaadinAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordRetrieval.class);
	private UnityMessageSource msg;
	private I18nString name;
	private String logoURL;
	private String registrationFormForUnknown;
	private boolean enableAssociation;
	private CredentialEditorRegistry credEditorReg;

	public PasswordRetrieval(UnityMessageSource msg, CredentialEditorRegistry credEditorReg)
	{
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
	}

	@Override
	public String getSerializedConfiguration()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		root.set("i18nName", I18nStringJsonUtil.toJson(name));
		root.put("registrationFormForUnknown", registrationFormForUnknown);
		root.put("enableAssociation", enableAssociation);
		if (logoURL != null)
			root.put("logoURL", logoURL);			
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize web-based password retrieval configuration to JSON", e);
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
				name = I18nDescribedObject.loadI18nStringFromBundle(
						"WebPasswordRetrieval.password", msg);
			JsonNode formNode = root.get("registrationFormForUnknown");
			if (formNode != null && !formNode.isNull())
				registrationFormForUnknown = formNode.asText();
			
			JsonNode logoNode = root.get("logoURL");
			if (logoNode != null && !logoNode.isNull())
				logoURL = logoNode.asText();
			if (logoURL != null && !logoURL.isEmpty())
				ImageUtils.getLogoResource(logoURL);
			
			JsonNode enableANode = root.get("enableAssociation");
			if (enableANode != null && !enableANode.isNull())
				enableAssociation = enableANode.asBoolean();
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based password retrieval can not be parsed or is invalid", e);
		}
	}

	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance()
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new PasswordRetrievalUI(credEditorReg.getEditor(PasswordVerificatorFactory.NAME)));
	}


	private class PasswordRetrievalComponent extends CustomComponent implements Focusable
	{
		private CredentialEditor credEditor;
		private AuthenticationResultCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		private String presetAuthenticatedIdentity;
		
		private TextField usernameField;
		private PasswordField passwordField;
		private int tabIndex;

		public PasswordRetrievalComponent(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
			initUI();
		}

		private void initUI()
		{
			VerticalLayout ret = new VerticalLayout();
			ret.setSpacing(true);
			
			usernameField = new TextField(msg.getMessage("AuthenticationUI.username"));
			usernameField.setId("AuthenticationUI.username");
			ret.addComponent(usernameField);
			
			String label = name.getValue(msg);
			passwordField = new PasswordField(label + ":");
			passwordField.setId("WebPasswordRetrieval.password");
			ret.addComponent(passwordField);

			if (credentialExchange.getCredentialResetBackend().getSettings().isEnabled())
			{
				Button reset = new Button(msg.getMessage("WebPasswordRetrieval.forgottenPassword"));
				reset.setStyleName(Styles.vButtonLink.toString());
				ret.addComponent(reset);
				ret.setComponentAlignment(reset, Alignment.TOP_RIGHT);
				reset.addClickListener(new ClickListener()
				{
					@Override
					public void buttonClick(ClickEvent event)
					{
						showResetDialog();
					}
				});
			}
			setCompositionRoot(ret);
		}

		public void triggerAuthentication()
		{
			String username = presetAuthenticatedIdentity == null ? usernameField.getValue() : 
				presetAuthenticatedIdentity;
			String password = passwordField.getValue();
			if (password.equals(""))
			{
				passwordField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.noPassword")));
			}
			if (username.equals(""))
			{
				usernameField.setComponentError(new UserError(
						msg.getMessage("WebPasswordRetrieval.noUser")));
			}			
			callback.setAuthenticationResult(getAuthenticationResult(username, password));
		}
		

		
		private AuthenticationResult getAuthenticationResult(String username, String password)
		{
			if (username.equals("") && password.equals(""))
			{
				return new AuthenticationResult(Status.notApplicable, null);
			}

			AuthenticationResult authenticationResult = credentialExchange.checkPassword(
						username, password, sandboxCallback);
			if (registrationFormForUnknown != null) 
				authenticationResult.setFormForUnknownPrincipal(registrationFormForUnknown);
			authenticationResult.setEnableAssociation(enableAssociation);
			if (authenticationResult.getStatus() == Status.success || 
					authenticationResult.getStatus() == Status.unknownRemotePrincipal)
			{
				clear();
			} else
			{
				setError();
			}
			return authenticationResult;
		}
		
		private void setError()
		{
			String msgErr = msg.getMessage("WebPasswordRetrieval.wrongPassword");
			passwordField.setComponentError(new UserError(msgErr));
			passwordField.setValue("");
			usernameField.setComponentError(new UserError(msgErr));
			usernameField.setValue("");
		}
		
		private void showResetDialog()
		{
			CredentialReset1Dialog dialog = new CredentialReset1Dialog(msg, 
					credentialExchange.getCredentialResetBackend(), credEditor);
			dialog.show();
		}

		@Override
		public void focus()
		{
			if (presetAuthenticatedIdentity == null)
				usernameField.focus();
			else
				passwordField.focus();
		}
		
		@Override
		public int getTabIndex()
		{
			return tabIndex;
		}

		@Override
		public void setTabIndex(int tabIndex)
		{
			this.tabIndex = tabIndex;
		}

		public void setCallback(AuthenticationResultCallback callback)
		{
			this.callback = callback;
		}

		public void setSandboxCallback(SandboxAuthnResultCallback sandboxCallback)
		{
			this.sandboxCallback = sandboxCallback;
		}
		
		public void setAuthenticatedIdentity(String authenticatedIdentity)
		{
			this.presetAuthenticatedIdentity = authenticatedIdentity;
			usernameField.setVisible(false);
		}

		public void clear()
		{
			passwordField.setValue("");
			usernameField.setValue("");
			passwordField.setComponentError(null);
			usernameField.setComponentError(null);
		}
	}
	
	private class PasswordRetrievalUI implements VaadinAuthenticationUI
	{
		private PasswordRetrievalComponent theComponent;


		public PasswordRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new PasswordRetrievalComponent(credEditor);
		}

		@Override
		public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
		{
			theComponent.setCallback(callback);
		}

		@Override
		public Component getComponent()
		{
			return theComponent;
		}

		@Override
		public void triggerAuthentication()
		{
			theComponent.triggerAuthentication();
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
				return Images.password.getResource();
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

		@Override
		public void cancelAuthentication()
		{
			//do nothing
		}

		@Override
		public void clear()
		{
			theComponent.clear();
		}

		@Override
		public void refresh(VaadinRequest request) 
		{
			//nop
		}

		@Override
		public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback) 
		{
			theComponent.setSandboxCallback(callback);
		}

		/**
		 * Simple: there is only one authN option in this authenticator so we can return any constant id. 
		 */
		@Override
		public String getId()
		{
			return "password";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
			Identity[] ids = authenticatedEntity.getIdentities();
			for (Identity id: ids)
				if (id.getTypeId().equals(UsernameIdentity.ID))
				{
					theComponent.setAuthenticatedIdentity(id.getValue());
					return;
				}
		}
	}
}










