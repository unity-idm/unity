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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.otp.credential_reset.OTPCredentialResetController;
import io.imunity.vaadin.auth.AuthNGridTextWrapper;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.elements.CssClassNames;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;

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

	private class OTPRetrievalComponent extends VerticalLayout implements Focusable<OTPRetrievalComponent>
	{
		private AuthenticationCallback callback;
		private TextField usernameField;
		private Span usernameLabel;
		private TextField codeField;
		private int tabIndex;
		private Entity presetEntity;

		private Button authenticateButton;

		private LinkButton lostDevice;
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

			usernameLabel = new Span("");
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
			authenticateButton.addClickListener(event -> triggerAuthentication());
			authenticateButton.setWidthFull();
			authenticateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			authenticateButton.addClassName(CssClassNames.SIGNIN_BUTTON.getName());
			authenticateButton.addClassName("u-otpSignInButton");
			

			codeField.addFocusListener(event ->
			{
				ShortcutRegistration shortcutRegistration = authenticateButton.addClickShortcut(Key.ENTER);
				codeField.addBlurListener(e -> shortcutRegistration.remove());
			});

			OTPResetSettings resetSettings = credentialExchange.getCredentialResetBackend().getResetSettings();
			if (resetSettings.enabled)
			{
				lostDevice = new LinkButton(
						msg.getMessage("OTPRetrieval.lostDevice"), event -> showResetDialog());
				add(new AuthNGridTextWrapper(lostDevice, Alignment.END));
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
			OTPCredentialResetController controller = new OTPCredentialResetController(msg,
					credentialExchange.getCredentialResetBackend(),
					credEditor, credResetLauncher.getConfiguration(), notificationPresenter);
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
		private final OTPRetrievalComponent theComponent;

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
			return new Image("assets/img/other/mobile-sms.png", "");
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
