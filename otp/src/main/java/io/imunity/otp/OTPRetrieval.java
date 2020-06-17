/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.types.basic.Identity;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Retrieves OTP code using a Vaadin textfield
 */
@PrototypeComponent
public class OTPRetrieval extends AbstractCredentialRetrieval<OTPExchange> implements VaadinAuthentication
{
	public static final String NAME = "web-otp";
	public static final String DESC = "OTPRetrievalFactory.desc";
	
	private MessageSource msg;
	private I18nString name;
	private CredentialEditorRegistry credEditorReg;
	private String configuration;
	
	@Autowired
	public OTPRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg)
	{	
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
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
			OTPRetrievalProperties config = new OTPRetrievalProperties(properties);
			name = config.getLocalizedString(msg, OTPRetrievalProperties.NAME);
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the OTP retrieval can not be parsed", e);
		}
	}
	
	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context)
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new OTPRetrievalUI(credEditorReg.getEditor(OTP.NAME)));
	}

	@Override
	public boolean supportsGrid()
	{
		return false;
	}
	
	private class OTPRetrievalComponent extends CustomComponent implements Focusable
	{
		private AuthenticationCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		private TextField usernameField;
		private HtmlLabel usernameLabel;
		private TextField codeField;
		private int tabIndex;
		private String presetUsername;
		
		private VerticalLayout mainLayout;
		
		private Button authenticateButton;

		//TODO
//		private Button lostPhone;
//		private CredentialEditor credEditor;
//		private CredentialResetLauncher credResetLauncher;
		
		public OTPRetrievalComponent(CredentialEditor credEditor)
		{
//			this.credEditor = credEditor;
			initUI();
		}

		private void initUI()
		{
			mainLayout = new VerticalLayout();
			mainLayout.setMargin(false);

			usernameField = new TextField();
			usernameField.setWidth(100, Unit.PERCENTAGE);
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addStyleName("u-authnTextField");
			usernameField.addStyleName("u-otpUsernameField");
			mainLayout.addComponent(usernameField);

			usernameLabel = new HtmlLabel(msg);
			mainLayout.addComponent(usernameLabel);
			usernameLabel.setVisible(false);

			codeField = new TextField();
			codeField.setWidth(100, Unit.PERCENTAGE);
			String codeLabel = name.isEmpty() ? 
					msg.getMessage("OTPRetrieval.code", credentialExchange.getCodeLength()) : 
					name.getValue(msg); 
			codeField.setPlaceholder(codeLabel);
			codeField.addStyleName("u-authnTextField");
			codeField.addStyleName("u-otpCodeField");
			mainLayout.addComponent(codeField);
			mainLayout.setComponentAlignment(codeField, Alignment.MIDDLE_CENTER);

			authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			mainLayout.addComponent(authenticateButton);
			authenticateButton.addClickListener(event -> {
				authenticateButton.removeClickShortcut();
				triggerAuthentication();
			});
			authenticateButton.addStyleName(Styles.signInButton.toString());
			authenticateButton.addStyleName("u-otpSignInButton");

			codeField.addFocusListener(e -> authenticateButton.setClickShortcut(KeyCode.ENTER));
			codeField.addBlurListener(e -> authenticateButton.removeClickShortcut());

			//TODO
//			SMSCredentialRecoverySettings settings = new SMSCredentialRecoverySettings(
//					JsonUtil.parse(credentialExchange
//							.getSMSCredentialResetBackend()
//							.getSettings()));

//			if (settings.isEnabled())
//			{
//				lostPhone = new Button(
//						msg.getMessage("WebSMSRetrieval.lostPhone"));
//				lostPhone.setStyleName(Styles.vButtonLink.toString());
//				mainLayout.addComponent(new AuthNGridTextWrapper(lostPhone, Alignment.TOP_RIGHT));
//				lostPhone.addClickListener(event -> showResetDialog());
//			}
			
			setCompositionRoot(mainLayout);
		}

		private void triggerAuthentication()
		{
			Optional<String> username = getUsername();
			if (!username.isPresent())
			{
				usernameField.focus();
				NotificationPopup.showError(msg.getMessage("OTPRetrieval.missingUsername"), "");
				return;
			}
			
			String code = codeField.getValue();
			if (Strings.isNullOrEmpty(code))
			{
				codeField.focus();
				NotificationPopup.showError(msg.getMessage("OTPRetrieval.missingCode"), "");
				return;
			}
				
			AuthenticationResult authnResult = credentialExchange.verifyCode(code, username.get(), sandboxCallback);
			setAuthenticationResult(authnResult);
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			clear();
			if (authenticationResult.getStatus() == Status.success)
			{
				setEnabled(false);
				callback.onCompletedAuthentication(authenticationResult);
			} else if (authenticationResult.getStatus() == Status.deny)
			{
				usernameField.focus();
				String msgErr = msg.getMessage("OTPRetrieval.wrongCode");
				callback.onFailedAuthentication(authenticationResult, msgErr, Optional.empty());
			} else
			{
				throw new IllegalStateException("Got unsupported status from verificator: " 
						+ authenticationResult.getStatus());
			}
		}
		
//		private void showResetDialog()
//		{
//			SMSCredentialResetController controller = new SMSCredentialResetController(msg,
//					credentialExchange.getSMSCredentialResetBackend(),
//					credEditor, credResetLauncher.getConfiguration());
//			credResetLauncher.startCredentialReset(controller.getInitialUI());
//		}

		@Override
		public void focus()
		{
			if (presetUsername == null)
				usernameField.focus();
			else
				codeField.focus();
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

		void setAuthenticatedIdentity(String authenticatedIdentity)
		{
			this.presetUsername = authenticatedIdentity;
			usernameField.setVisible(false);
			usernameLabel.setVisible(true);
		}

		private void clear()
		{
			usernameField.setValue("");
			codeField.setValue("");
		}

		void hideCredentialReset()
		{
			//TODO
//			if (lostPhone != null)
//				lostPhone.setVisible(false);
		}

		void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
//			this.credResetLauncher = credResetLauncher;
			
		}
		
		private Optional<String> getUsername()
		{
			if (!Strings.isNullOrEmpty(presetUsername))
				return Optional.of(presetUsername);
			String enteredUsername = usernameField.getValue();
			if (!Strings.isNullOrEmpty(enteredUsername))
				return Optional.of(enteredUsername);
			return Optional.empty();
		}
	}

	private class OTPRetrievalUI implements VaadinAuthenticationUI
	{
		private OTPRetrievalComponent theComponent;

		public OTPRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new OTPRetrievalComponent(credEditor);
		}

		@Override
		public void setAuthenticationCallback(AuthenticationCallback callback)
		{
			theComponent.setCallback(callback);
		}

		@Override
		public void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
			theComponent.setCredentialResetLauncher(credResetLauncher);
		}
		
		@Override
		public Component getComponent()
		{
			return theComponent;
		}

		@Override
		public String getLabel()
		{
			return name.getValue(msg); //not fully correct (no fallback to default) but we don't support grid so irrelevant.
		}

		@Override
		public Resource getImage()
		{
			return Images.otp.getResource();
		}

		@Override
		public void clear()
		{
			theComponent.clear();
		}

		@Override
		public void refresh(VaadinRequest request)
		{
			// nop
		}

		@Override
		public void setSandboxAuthnCallback(SandboxAuthnResultCallback callback)
		{
			theComponent.setSandboxCallback(callback);
		}

		/**
		 * Simple: there is only one authN option in this authenticator
		 * so we can return any constant id.
		 */
		@Override
		public String getId()
		{
			return "otp";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
			List<Identity> ids = authenticatedEntity.getIdentities();
			Optional<Identity> userNameId = ids.stream().filter(id -> id.getTypeId().equals(UsernameIdentity.ID)).findAny();
			if (userNameId.isPresent())
			{
				theComponent.setAuthenticatedIdentity(userNameId.get().getValue());
				return;
			}
			Optional<Identity> emailId = ids.stream().filter(id -> id.getTypeId().equals(EmailIdentity.ID)).findAny();
			if (emailId.isPresent())
			{
				theComponent.setAuthenticatedIdentity(emailId.get().getValue());
				return;
			}
		}

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}

		@Override
		public void disableCredentialReset()
		{
			theComponent.hideCredentialReset();
		}
	}

	@org.springframework.stereotype.Component
	public static class Factory extends AbstractCredentialRetrievalFactory<OTPRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<OTPRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, OTPExchange.ID);
		}
	}
}
