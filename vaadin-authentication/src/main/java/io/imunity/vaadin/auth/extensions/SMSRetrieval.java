/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.vaadin.auth.extensions;

import com.google.common.base.Strings;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.ShortcutRegistration;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.vaadin.auth.AuthNGridTextWrapper;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.VaadinAuthentication;
import io.imunity.vaadin.auth.extensions.credreset.sms.SMSCredentialResetController;
import io.imunity.vaadin.elements.LinkButton;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.forms.components.CaptchaComponent;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditorRegistry;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pl.edu.icm.unity.base.entity.Entity;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.i18n.I18nString;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.*;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.stdext.credential.sms.SMSExchange;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;

import java.io.StringReader;
import java.util.*;

@PrototypeComponent
public class SMSRetrieval extends AbstractCredentialRetrieval<SMSExchange> implements VaadinAuthentication
{
	private final Logger log = Log.getLogger(Log.U_SERVER_WEB, SMSRetrieval.class);
	public static final String NAME = "vaadin-sms";
	public static final String DESC = "WebSMSRetrievalFactory.desc";
	
	private final MessageSource msg;
	private I18nString name;
	private final CredentialEditorRegistry credEditorReg;
	private final NotificationPresenter notificationPresenter;
	private String configuration;
	
	@Autowired
	public SMSRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg, NotificationPresenter notificationPresenter)
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
			SMSRetrievalProperties config = new SMSRetrievalProperties(properties);
			name = config.getLocalizedString(msg, SMSRetrievalProperties.NAME);
			if (name.isEmpty())
				name = new I18nString("WebSMSRetrieval.title", msg);
	
		} catch (Exception e)
		{
			throw new ConfigurationException("The configuration of the web-" +
					"based SMS retrieval can not be parsed", e);
		}
	}
	
	@Override
	public Collection<VaadinAuthenticationUI> createUIInstance(Context context, AuthenticatorStepContext authenticatorContext)
	{
		return Collections.singleton(
				new SMSRetrievalUI(credEditorReg.getEditor(SMSVerificator.NAME), notificationPresenter));
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

	private class SMSRetrievalComponent extends VerticalLayout implements Focusable<SMSRetrievalComponent>
	{
		private final CredentialEditor credEditor;
		private AuthenticationCallback callback;
		private TextField usernameField;
		private Span usernameLabel;
		private TextField answerField;
		private int tabIndex;
		private SMSCode sentCode = null;
		private Button sendCodeButton;
		private Button resetButton;
		private Entity presetEntity;
		private CaptchaComponent capcha;
		private VerticalLayout capchaComponent;
		private Span capchaInfoLabel;
		private Button authenticateButton;
		private LinkButton lostPhone;
		private CredentialResetLauncher credResetLauncher;
		private final NotificationPresenter notificationPresenter;

		public SMSRetrievalComponent(CredentialEditor credEditor, NotificationPresenter notificationPresenter)
		{
			this.credEditor = credEditor;
			this.notificationPresenter = notificationPresenter;
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
			usernameField.addClassName("u-smsUsernameField");
			add(usernameField);

			usernameLabel = new Span();
			add(usernameLabel);
			usernameLabel.setVisible(false);

			capcha = new CaptchaComponent(msg, 6, false);
			capchaInfoLabel = new Span();
			capchaComponent = new VerticalLayout();
			capchaComponent.setMargin(false);
			capchaComponent.setPadding(false);
			capchaComponent.add(capchaInfoLabel);
			Component rCapchaComponent = capcha.getAsComponent(Alignment.CENTER);
			capchaComponent.add(rCapchaComponent);
			capchaComponent.setAlignItems(Alignment.CENTER);
			add(capchaComponent);
			capchaComponent.setVisible(false);
		
			sendCodeButton = new Button(msg.getMessage("WebSMSRetrieval.sendCode"));
			Icon icon = VaadinIcon.MOBILE_RETRO.create();
			icon.setColor("white");
			sendCodeButton.setIcon(icon);
			sendCodeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			sendCodeButton.setWidthFull();
			sendCodeButton.addClickListener(e -> sendCode());
			usernameField.addFocusListener(event ->
			{
				ShortcutRegistration shortcutRegistration = sendCodeButton.addClickShortcut(Key.ENTER);
				usernameField.addBlurListener(e -> shortcutRegistration.remove());
			});

			add(sendCodeButton);

			resetButton = new Button(msg.getMessage("WebSMSRetrieval.reset"));
			resetButton.setIcon(VaadinIcon.BAN.create());
			resetButton.setWidthFull();
			resetButton.addClassName("u-smsResetButton");
			resetButton.addClickListener(e -> {
				callback.onCancelledAuthentication();
				resetSentCode();
				usernameField.focus();
			});
			resetButton.setVisible(false);

			add(resetButton);

			answerField = new TextField();
			answerField.setWidthFull();
			answerField.setEnabled(false);
			answerField.setPlaceholder(msg.getMessage("WebSMSRetrieval.code"));
			answerField.addClassName("u-authnTextField");
			answerField.addClassName("u-smsCodeField");
			add(answerField);
			setAlignItems(Alignment.CENTER);

			authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			add(authenticateButton);
			authenticateButton.addClickListener(event -> triggerAuthentication());
			authenticateButton.addClassName("u-smsSignInButton");
			authenticateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
			authenticateButton.setWidthFull();
			authenticateButton.setEnabled(false);

			answerField.addFocusListener(event ->
			{
				ShortcutRegistration shortcutRegistration = authenticateButton.addClickShortcut(Key.ENTER);
				answerField.addBlurListener(e -> shortcutRegistration.remove());
			});

			SMSCredentialRecoverySettings settings = new SMSCredentialRecoverySettings(
					JsonUtil.parse(credentialExchange
							.getSMSCredentialResetBackend()
							.getSettings()));

			if (settings.isEnabled())
			{
				lostPhone = new LinkButton(
						msg.getMessage("WebSMSRetrieval.lostPhone"), event -> showResetDialog());
				add(new AuthNGridTextWrapper(lostPhone, Alignment.END));
			}
		}

		private void resetSentCode()
		{
			sendCodeButton.setVisible(true);
			resetButton.setVisible(false);
			capchaComponent.setVisible(false);
			usernameField.setVisible(true);
			usernameField.setValue("");
			usernameLabel.setVisible(false);
			usernameLabel.setText("");
			answerField.setValue("");
			answerField.setEnabled(false);
			authenticateButton.setEnabled(false);
			sentCode = null;
		}

		private void sendCode()
		{
			boolean force = false;
			Optional<AuthenticationSubject> subjectOpt = getAuthenticationSubject();
			if (subjectOpt.isEmpty())
			{
				notificationPresenter.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"),
						msg.getMessage("WebSMSRetrieval.noUser"));
				return;
			}
			AuthenticationSubject subject = subjectOpt.get();
			
			usernameField.setVisible(false);
		
			usernameLabel.setVisible(true);
			resetButton.setVisible(true);			
			
			
			if (credentialExchange.isAuthSMSLimitExceeded(subject)
					&& !capchaComponent.isVisible())
			{
				capchaInfoLabel.setText(msg.getMessage(
						"WebSMSRetrieval.sentCodeLimit"));
				capchaComponent.setVisible(true);
				capcha.reset();
				usernameLabel.setText("");
				sendCodeButton.setVisible(true);
				log.info("Too many authn sms code sent to the user, turn on capcha");
				return;
			}
					
			if (capchaComponent.isVisible())
			{
				try
				{
					capcha.verify();
					force = true;
				} catch (WrongArgumentException e)
				{
					return;
				}
			}
			
			try
			{
				sentCode = credentialExchange.sendCode(subject, force);
				
			} catch (EngineException e)
			{
				log.warn("Cannot send authn sms code", e);
				notificationPresenter.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"),
						msg.getMessage("WebSMSRetrieval.cannotSendSMS"));
				return;
			}
			
			usernameLabel.setText(msg.getMessage("WebSMSRetrieval.usernameLabelCodeSent"));
			capcha.reset();
			answerField.setEnabled(true);		
			authenticateButton.setEnabled(true);
			capchaComponent.setVisible(false);
			sendCodeButton.setVisible(false);
			answerField.focus();
			if (callback != null)
				callback.onStartedAuthentication();
		}

		private void triggerAuthentication()
		{
			Optional<AuthenticationSubject> subjectOpt = getAuthenticationSubject();
			if (subjectOpt.isEmpty())
			{
				setAuthenticationResult(LocalAuthenticationResult.notApplicable());
				return;
			}
			setAuthenticationResult(credentialExchange.verifyCode(sentCode,
					answerField.getValue(), subjectOpt.get()));
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			if (authenticationResult.getStatus() == Status.success)
			{
				clear();
				setEnabled(false);
				callback.onCompletedAuthentication(authenticationResult, getContext());
			} else if (authenticationResult.getStatus() == Status.unknownRemotePrincipal)
			{
				clear();
				callback.onCompletedAuthentication(authenticationResult, getContext());
			} else
			{
				setError();
				usernameField.focus();
				callback.onCompletedAuthentication(authenticationResult, getContext());
			}
		}
		
		private void setError()
		{
			resetSentCode();
			usernameField.setValue("");
			answerField.setValue("");
		}

		private void showResetDialog()
		{
			SMSCredentialResetController controller = new SMSCredentialResetController(msg,
					credentialExchange.getSMSCredentialResetBackend(),
					credEditor, credResetLauncher.getConfiguration(), notificationPresenter);
			Optional<AuthenticationSubject> presetSubject = presetEntity == null ?
					Optional.empty() : Optional.of(AuthenticationSubject.entityBased(presetEntity.getId()));
			credResetLauncher.startCredentialReset(controller.getInitialUI(presetSubject));
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
		
		@Override
		public void focus()
		{
			if (presetEntity == null)
				usernameField.focus();
			else
				answerField.focus();
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

		void setAuthenticatedIdentity(Entity authenticatedIdentity)
		{
			this.presetEntity = authenticatedIdentity;
			sendCodeButton.setVisible(false);
			remove(usernameField);
			remove(resetButton);
			remove(usernameLabel);
			sendCode();
		}

		private void clear()
		{
			resetSentCode();
			usernameField.setValue("");
			answerField.setValue("");
		}

		void hideLostPhone()
		{
			if (lostPhone != null)
				lostPhone.setVisible(false);
		}

		void setCredentialResetLauncher(CredentialResetLauncher credResetLauncher)
		{
			this.credResetLauncher = credResetLauncher;
			
		}
	}

	private class SMSRetrievalUI implements VaadinAuthenticationUI
	{
		private final SMSRetrievalComponent theComponent;

		public SMSRetrievalUI(CredentialEditor credEditor, NotificationPresenter notificationPresenter)
		{
			this.theComponent = new SMSRetrievalComponent(credEditor, notificationPresenter);
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
			return name.getValue(msg);
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
			return "sms";
		}

		@Override
		public void presetEntity(Entity authenticatedEntity)
		{
			theComponent.setAuthenticatedIdentity(authenticatedEntity);
		}

		@Override
		public Set<String> getTags()
		{
			return Collections.emptySet();
		}

		@Override
		public void disableCredentialReset()
		{
			theComponent.hideLostPhone();
		}
	}

	@org.springframework.stereotype.Component
	public static class Factory extends AbstractCredentialRetrievalFactory<SMSRetrieval>
	{
		@Autowired
		public Factory(ObjectFactory<SMSRetrieval> factory)
		{
			super(NAME, DESC, VaadinAuthentication.NAME, factory, SMSExchange.ID);
		}
	}
}
