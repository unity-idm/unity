/*
 * Copyright (c) 2020 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.otp;

import com.google.common.base.Strings;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.v8.OTPExchange;
import io.imunity.otp.v8.OTPRetrievalProperties;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import io.imunity.vaadin.auth.AuthNGridTextWrapper;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.VaadinAuthentication;

import java.io.StringReader;
import java.util.*;

/**
 * Retrieves OTP code using a Vaadin textfield
 */
@PrototypeComponent
class OTPRetrieval extends AbstractCredentialRetrieval<OTPExchange> implements VaadinAuthentication
{
	static final String NAME = "vaadin-otp";
	static final String DESC = "OTPRetrievalFactory.desc";
	
	private final MessageSource msg;
	private final CredentialEditorRegistry credEditorReg;
	private final NotificationPresenter notificationPresenter;
	private I18nString name;
	private String configuration;
	
	@Autowired
	public OTPRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg, NotificationPresenter notificationPresenter)
	{	
		super(VaadinAuthentication.NAME);
		this.msg = msg;
		this.credEditorReg = credEditorReg;
		this.notificationPresenter = notificationPresenter;
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
		return Collections.singleton(
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

	private AuthenticationRetrievalContext getContext()
	{
		return AuthenticationRetrievalContext.builder().withSupportOnlySecondFactorReseting(true).build();
	}

	private class OTPRetrievalComponent extends VerticalLayout implements Focusable
	{
		private AuthenticationCallback callback;
		private TextField usernameField;
		private Label usernameLabel;
		private TextField codeField;
		private int tabIndex;
		private Entity presetEntity;

		private Button authenticateButton;

		private Button lostDevice;
		private final CredentialEditor credEditor;
		private CredentialResetLauncher credResetLauncher;
		
		public OTPRetrievalComponent(CredentialEditor credEditor)
		{
			this.credEditor = credEditor;
			initUI();
		}

		private void initUI()
		{
			setMargin(false);
			setPadding(false);
			getStyle().set("gap", "0");

			usernameField = new TextField();
			usernameField.setWidthFull();
			usernameField.setPlaceholder(msg.getMessage("AuthenticationUI.username"));
			usernameField.addClassName("u-authnTextField");
			usernameField.addClassName("u-otpUsernameField");
			add(usernameField);

			usernameLabel = new Label("");
			add(usernameLabel);
			usernameLabel.setVisible(false);

			codeField = new TextField();
			codeField.setWidthFull();
			String codeLabel = name.isEmpty() ? 
					msg.getMessage("OTPRetrieval.code", credentialExchange.getCodeLength()) : 
					name.getValue(msg); 
			codeField.setPlaceholder(codeLabel);
			codeField.addClassName("u-authnTextField");
			codeField.addClassName("u-otpCodeField");
			add(codeField);
			setAlignItems(Alignment.CENTER);

			authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			add(authenticateButton);
			authenticateButton.addClickListener(event -> {
				triggerAuthentication();
			});
			authenticateButton.setWidthFull();
			authenticateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			authenticateButton.addClassName("u-otpSignInButton");

			codeField.addFocusListener(event ->
			{
				ShortcutRegistration shortcutRegistration = authenticateButton.addClickShortcut(Key.ENTER);
				codeField.addBlurListener(e -> shortcutRegistration.remove());
			});

			OTPResetSettings resetSettings = credentialExchange.getCredentialResetBackend().getResetSettings();
			if (resetSettings.enabled)
			{
				lostDevice = new Button(
						msg.getMessage("OTPRetrieval.lostDevice"));
				add(new AuthNGridTextWrapper(lostDevice, Alignment.END));
				lostDevice.addClickListener(event -> showResetDialog());
			}
		}

		private void triggerAuthentication()
		{
			Optional<AuthenticationSubject> subject = getAuthenticationSubject();
			if (subject.isEmpty())
			{
				usernameField.focus();
				notificationPresenter.showError(msg.getMessage("OTPRetrieval.missingUsername"), "");
				return;
			}
			
			String code = codeField.getValue();
			if (Strings.isNullOrEmpty(code))
			{
				codeField.focus();
				notificationPresenter.showError(msg.getMessage("OTPRetrieval.missingCode"), "");
				return;
			}
				
			AuthenticationResult authnResult = credentialExchange.verifyCode(code, subject.get());
			setAuthenticationResult(authnResult);
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			clear();
			if (authenticationResult.getStatus() == Status.success)
			{
				setEnabled(false);
				callback.onCompletedAuthentication(authenticationResult, getContext());
			} else if (authenticationResult.getStatus() == Status.deny)
			{
				usernameField.focus();
				callback.onCompletedAuthentication(authenticationResult, getContext());
			} else
			{
				throw new IllegalStateException("Got unsupported status from verificator: " 
						+ authenticationResult.getStatus());
			}
		}
		
		private void showResetDialog()
		{
//			FIXME UY-1335 Support credential reset
//			OTPCredentialResetController controller = new OTPCredentialResetController(msg,
//					credentialExchange.getCredentialResetBackend(),
//					credEditor, credResetLauncher.getConfiguration());
//			Optional<AuthenticationSubject> presetSubject = presetEntity == null ?
//					Optional.empty() : Optional.of(AuthenticationSubject.entityBased(presetEntity.getId()));
//			credResetLauncher.startCredentialReset(controller.getInitialUI(presetSubject));
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
		public Image getImage()
		{
			return new Image("../unitygw/img/other/mobile-sms.png", "");
		}

		@Override
		public void clear()
		{
			theComponent.clear();
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
