/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Strings;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Component.Focusable;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.resetui.OTPCredentialResetController;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.authn.AuthNGridTextWrapper;
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
class OTPRetrieval extends AbstractCredentialRetrieval<OTPExchange> implements VaadinAuthentication
{
	static final String NAME = "web-otp";
	static final String DESC = "OTPRetrievalFactory.desc";
	
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
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authenticatorContext)
	{
		return Collections.<VaadinAuthenticationUI>singleton(
				new OTPRetrievalUI(credEditorReg.getEditor(OTP.NAME)));
	}

	@Override
	public boolean supportsGrid()
	{
		return false;
	}

	@Override
	public boolean isMultiOption()
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
		private Entity presetEntity;
		
		private VerticalLayout mainLayout;
		
		private Button authenticateButton;

		private Button lostDevice;
		private CredentialEditor credEditor;
		private CredentialResetLauncher credResetLauncher;
		
		public OTPRetrievalComponent(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
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

			OTPResetSettings resetSettings = credentialExchange.getCredentialResetBackend().getResetSettings();
			if (resetSettings.enabled)
			{
				lostDevice = new Button(
						msg.getMessage("OTPRetrieval.lostDevice"));
				lostDevice.setStyleName(Styles.vButtonLink.toString());
				mainLayout.addComponent(new AuthNGridTextWrapper(lostDevice, Alignment.TOP_RIGHT));
				lostDevice.addClickListener(event -> showResetDialog());
			}
			
			setCompositionRoot(mainLayout);
		}

		private void triggerAuthentication()
		{
			Optional<AuthenticationSubject> subject = getAuthenticationSubject();
			if (!subject.isPresent())
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
				
			AuthenticationResult authnResult = credentialExchange.verifyCode(code, subject.get(), sandboxCallback);
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
				callback.onCompletedAuthentication(authenticationResult);
			} else
			{
				throw new IllegalStateException("Got unsupported status from verificator: " 
						+ authenticationResult.getStatus());
			}
		}
		
		private void showResetDialog()
		{
			OTPCredentialResetController controller = new OTPCredentialResetController(msg,
					credentialExchange.getCredentialResetBackend(),
					credEditor, credResetLauncher.getConfiguration());
			Optional<AuthenticationSubject> presetSubject = presetEntity == null ? 
					Optional.empty() : Optional.of(AuthenticationSubject.entityBased(presetEntity.getId()));
			credResetLauncher.startCredentialReset(controller.getInitialUI(presetSubject));
		}

		@Override
		public void focus()
		{
			if (presetEntity == null)
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

		void setAuthenticatedEntity(Entity authenticatedEntity)
		{
			this.presetEntity = authenticatedEntity;
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
			if (lostDevice != null)
				lostDevice.setVisible(false);
		}

		void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
			this.credResetLauncher = credResetLauncher;
		}
		
		private Optional<AuthenticationSubject> getAuthenticationSubject()
		{
			if (presetEntity != null)
				return Optional.of(AuthenticationSubject.entityBased(presetEntity.getId()));
			String enteredUsername = usernameField.getValue();
			if (!Strings.isNullOrEmpty(enteredUsername))
				return Optional.of(AuthenticationSubject.identityBased(enteredUsername));
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
			theComponent.setAuthenticatedEntity(authenticatedEntity);
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
	static class Factory extends AbstractCredentialRetrievalFactory<OTPRetrieval>
	{
		@Autowired
		Factory(ObjectFactory<OTPRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, OTPExchange.ID);
		}
	}
}
