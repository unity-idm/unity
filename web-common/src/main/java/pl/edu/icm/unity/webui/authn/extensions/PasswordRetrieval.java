/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.extensions;

import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationException;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordExchange;
import pl.edu.icm.unity.stdext.credential.pass.PasswordVerificator;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.I18nStringJsonUtil;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.authn.AuthNGridTextWrapper;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.password.PasswordCredentialReset1Dialog;
import pl.edu.icm.unity.webui.common.ImageUtils;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;

/**
 * Retrieves passwords using a Vaadin widget.
 * 
 * @author K. Benedyczak
 */
@PrototypeComponent
public class PasswordRetrieval extends AbstractCredentialRetrieval<PasswordExchange> implements VaadinAuthentication
{
	public static final String NAME = "web-password";
	public static final String DESC = "WebPasswordRetrievalFactory.desc";
	
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordRetrieval.class);
	private UnityMessageSource msg;
	private I18nString name;
	private String logoURL;
	private String registrationFormForUnknown;
	private boolean enableAssociation;
	private CredentialEditorRegistry credEditorReg;

	@Autowired
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
				name = new I18nString("WebPasswordRetrieval.password", msg);
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
				new PasswordRetrievalUI(credEditorReg.getEditor(PasswordVerificator.NAME)));
	}


	private class PasswordRetrievalComponent extends CustomComponent implements Focusable
	{
		private CredentialEditor credEditor;
		private AuthenticationCallback callback;
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
			ret.setMargin(false);
			
			usernameField = new TextField();
			usernameField.setWidth(100, Unit.PERCENTAGE);
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			ret.addComponent(usernameField);
			
			String label = name.getValue(msg);
			passwordField = new PasswordField();
			passwordField.setWidth(100, Unit.PERCENTAGE);
			passwordField.setPlaceholder(label);
			ret.addComponent(passwordField);
			
			
			Button authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			authenticateButton.addStyleName(Styles.signInButton.toString());
			authenticateButton.addClickListener(event -> triggerAuthentication());
			ret.addComponent(authenticateButton);

			passwordField.addFocusListener(e -> authenticateButton.setClickShortcut(KeyCode.ENTER));
			passwordField.addBlurListener(e -> authenticateButton.removeClickShortcut());

			PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(
					JsonUtil.parse(credentialExchange
							.getCredentialResetBackend()
							.getSettings()));
			if (settings.isEnabled())
			{
				Button reset = new Button(msg.getMessage("WebPasswordRetrieval.forgottenPassword"));
				reset.setStyleName(Styles.vButtonLink.toString());
				AuthNGridTextWrapper resetWrapper = new AuthNGridTextWrapper(reset, Alignment.TOP_RIGHT);
				resetWrapper.addStyleName("u-authn-forgotPassword");
				ret.addComponent(resetWrapper);
				reset.addClickListener(event -> showResetDialog());
			}
			
			setCompositionRoot(ret);
		}

		private void triggerAuthentication()
		{
			String username = presetAuthenticatedIdentity == null ? usernameField.getValue() : 
				presetAuthenticatedIdentity;
			String password = passwordField.getValue();

			if (password.equals(""))
			{
				NotificationPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), 
						msg.getMessage("WebPasswordRetrieval.noPassword"));
			} else if (username.equals(""))
			{
				NotificationPopup.showError(msg, msg.getMessage("AuthenticationUI.authnErrorTitle"), 
						msg.getMessage("WebPasswordRetrieval.noUser"));
			} else 
			{
				callback.onStartedAuthentication(AuthenticationStyle.IMMEDIATE);
				AuthenticationResult authenticationResult = getAuthenticationResult(username, password);
				if (authenticationResult.getStatus() == Status.deny)
				{
					callback.onFailedAuthentication(authenticationResult, 
							msg.getMessage("WebPasswordRetrieval.wrongPassword"), 
							Optional.empty());
				} else
				{
					callback.onCompletedAuthentication(authenticationResult);
				}
			}
		}
		
		private AuthenticationResult getAuthenticationResult(String username, String password)
		{
			if (username.equals("") && password.equals(""))
			{
				return new AuthenticationResult(Status.notApplicable, null);
			}

			
			AuthenticationResult authenticationResult;
			try
			{
				authenticationResult = credentialExchange.checkPassword(
						username, password, sandboxCallback);
			} catch (AuthenticationException e)
			{
				log.debug("Authentication error during password checking", e);
				authenticationResult = e.getResult();
			} catch (Exception e)
			{
				log.error("Runtime error during password checking", e);
				authenticationResult = new AuthenticationResult(Status.deny, null);
			}
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
			passwordField.setValue("");
			usernameField.setValue("");
		}
		
		private void showResetDialog()
		{
			PasswordCredentialReset1Dialog dialog = new PasswordCredentialReset1Dialog(msg, 
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

		public void setCallback(AuthenticationCallback callback)
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
		public void setAuthenticationCallback(AuthenticationCallback callback)
		{
			theComponent.setCallback(callback);
		}

		@Override
		public Component getComponent()
		{
			return theComponent;
		}

		@Override
		public String getLabel()
		{
			return name.getValue(msg);
		}

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
		public void setSandboxAuthnCallback(SandboxAuthnResultCallback callback) 
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
			List<Identity> ids = authenticatedEntity.getIdentities();
			for (Identity id: ids)
				if (id.getTypeId().equals(UsernameIdentity.ID))
				{
					theComponent.setAuthenticatedIdentity(id.getValue());
					return;
				}
		}

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}
	}
	
	
	@org.springframework.stereotype.Component
	public static class Factory extends AbstractCredentialRetrievalFactory<PasswordRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<PasswordRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, PasswordExchange.class);
		}
	}
}


