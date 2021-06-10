/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.webui.authn.extensions;

import java.io.StringReader;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.Logger;
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
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrievalFactory;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.AuthenticatorStepContext;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.stdext.credential.sms.SMSExchange;
import pl.edu.icm.unity.stdext.credential.sms.SMSVerificator;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.Entity;
import pl.edu.icm.unity.webui.authn.AuthNGridTextWrapper;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication;
import pl.edu.icm.unity.webui.authn.credreset.sms.SMSCredentialResetController;
import pl.edu.icm.unity.webui.common.CaptchaComponent;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditorRegistry;
import pl.edu.icm.unity.webui.common.safehtml.HtmlLabel;

/**
 * Retrieves sms code using a Vaadin widget.
 * @author P.Piernik
 *
 */
@PrototypeComponent
public class SMSRetrieval extends AbstractCredentialRetrieval<SMSExchange> implements VaadinAuthentication
{
	private Logger log = Log.getLogger(Log.U_SERVER_WEB, SMSRetrieval.class);
	public static final String NAME = "web-sms";
	public static final String DESC = "WebSMSRetrievalFactory.desc";
	
	private MessageSource msg;
	private I18nString name;
	private CredentialEditorRegistry credEditorReg;
	private String configuration;
	
	@Autowired
	public SMSRetrieval(MessageSource msg, CredentialEditorRegistry credEditorReg)
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
		return Collections.<VaadinAuthenticationUI>singleton(
				new SMSRetrievalUI(credEditorReg.getEditor(SMSVerificator.NAME)));
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

	private class SMSRetrievalComponent extends CustomComponent implements Focusable
	{
		private CredentialEditor credEditor;
		private AuthenticationCallback callback;
		private SandboxAuthnResultCallback sandboxCallback;
		private TextField usernameField;
		private HtmlLabel usernameLabel;
		private TextField answerField;
		private int tabIndex;
		private SMSCode sentCode = null;
		private Button sendCodeButton;
		private Button resetButton;
		private Entity presetEntity;
		private CaptchaComponent capcha;
		private VerticalLayout capchaComponent;
		private Label capchaInfoLabel;
		private VerticalLayout mainLayout;
		private Button authenticateButton;
		private Button lostPhone;
		private CredentialResetLauncher credResetLauncher;
		
		public SMSRetrievalComponent(CredentialEditor credEditor)
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
			usernameField.addStyleName("u-smsUsernameField");
			mainLayout.addComponent(usernameField);

			usernameLabel = new HtmlLabel(msg);
			mainLayout.addComponent(usernameLabel);
			usernameLabel.setVisible(false);

			capcha = new CaptchaComponent(msg, false);
			capchaInfoLabel = new Label();
			capchaComponent = new VerticalLayout();
			capchaComponent.setMargin(false);
			capchaComponent.addComponent(capchaInfoLabel);
			capchaComponent.setComponentAlignment(capchaInfoLabel,
					Alignment.MIDDLE_CENTER);
			Component rCapchaComponent = capcha.getAsComponent(Alignment.MIDDLE_CENTER);
			capchaComponent.addComponent(rCapchaComponent);
			capchaComponent.setComponentAlignment(rCapchaComponent,
					Alignment.MIDDLE_CENTER);
			mainLayout.addComponent(capchaComponent);
			capchaComponent.setVisible(false);
		
			sendCodeButton = new Button(msg.getMessage("WebSMSRetrieval.sendCode"));
			sendCodeButton.setIcon(Images.mobile.getResource());
			sendCodeButton.addStyleName(Styles.signInButton.toString());
			sendCodeButton.addClickListener(e -> 
			{
				sendCodeButton.removeClickShortcut();
				sendCode();
			});
			usernameField.addFocusListener(e -> sendCodeButton.setClickShortcut(KeyCode.ENTER));
			usernameField.addBlurListener(e -> sendCodeButton.removeClickShortcut());
			
			mainLayout.addComponent(sendCodeButton);

			resetButton = new Button(msg.getMessage("WebSMSRetrieval.reset"));
			resetButton.setIcon(Images.reject.getResource());
			resetButton.setWidth(100, Unit.PERCENTAGE);
			resetButton.addStyleName("u-smsResetButton");
			resetButton.addClickListener(e -> {
				sendCodeButton.removeClickShortcut();
				authenticateButton.removeClickShortcut();
				callback.onCancelledAuthentication();
				resetSentCode();
				usernameField.focus();
			});
			resetButton.setVisible(false);

			mainLayout.addComponent(resetButton);

			answerField = new TextField();
			answerField.setWidth(100, Unit.PERCENTAGE);
			answerField.setPlaceholder(msg.getMessage("WebSMSRetrieval.code"));
			answerField.setEnabled(false);
			answerField.addStyleName("u-authnTextField");
			answerField.addStyleName("u-smsCodeField");
			mainLayout.addComponent(answerField);
			mainLayout.setComponentAlignment(answerField, Alignment.MIDDLE_CENTER);

			authenticateButton = new Button(msg.getMessage("AuthenticationUI.authnenticateButton"));
			mainLayout.addComponent(authenticateButton);
			authenticateButton.addClickListener(event -> {
				sendCodeButton.removeClickShortcut();
				authenticateButton.removeClickShortcut();
				triggerAuthentication();
			});
			authenticateButton.addStyleName(Styles.signInButton.toString());
			authenticateButton.addStyleName("u-smsSignInButton");
			authenticateButton.setEnabled(false);

			answerField.addFocusListener(e -> authenticateButton.setClickShortcut(KeyCode.ENTER));
			answerField.addBlurListener(e -> authenticateButton.removeClickShortcut());

			SMSCredentialRecoverySettings settings = new SMSCredentialRecoverySettings(
					JsonUtil.parse(credentialExchange
							.getSMSCredentialResetBackend()
							.getSettings()));

			if (settings.isEnabled())
			{
				lostPhone = new Button(
						msg.getMessage("WebSMSRetrieval.lostPhone"));
				lostPhone.setStyleName(Styles.vButtonLink.toString());
				mainLayout.addComponent(new AuthNGridTextWrapper(lostPhone, Alignment.TOP_RIGHT));
				lostPhone.addClickListener(event -> showResetDialog());
			}
			
			setCompositionRoot(mainLayout);
		}

		private void resetSentCode()
		{
			sendCodeButton.setVisible(true);
			resetButton.setVisible(false);
			capchaComponent.setVisible(false);
			usernameField.setVisible(true);
			usernameField.setValue("");
			usernameLabel.setVisible(false);
			usernameLabel.resetValue();
			answerField.setValue("");
			answerField.setEnabled(false);
			authenticateButton.setEnabled(false);
			sentCode = null;
		}

		private void sendCode()
		{
			boolean force = false;
			Optional<AuthenticationSubject> subjectOpt = getAuthenticationSubject();
			if (!subjectOpt.isPresent())
			{
				NotificationPopup.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"), 
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
				capchaInfoLabel.setValue(msg.getMessage(
						"WebSMSRetrieval.sentCodeLimit"));
				capchaComponent.setVisible(true);
				capcha.resetFull();
				usernameLabel.setValue("");
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
				NotificationPopup.showError(msg.getMessage("AuthenticationUI.authnErrorTitle"), 
						msg.getMessage("WebSMSRetrieval.cannotSendSMS"));
				return;
			}
			
			usernameLabel.setHtmlValue("WebSMSRetrieval.usernameLabelCodeSent");
			capcha.reset();
			answerField.setEnabled(true);		
			authenticateButton.setEnabled(true);
			capchaComponent.setVisible(false);
			sendCodeButton.setVisible(false);
			answerField.focus();
			if (callback != null)
				callback.onStartedAuthentication(AuthenticationStyle.WITH_EMBEDDED_CANCEL);
		}

		private void triggerAuthentication()
		{
			Optional<AuthenticationSubject> subjectOpt = getAuthenticationSubject();
			if (!subjectOpt.isPresent())
			{
				setAuthenticationResult(LocalAuthenticationResult.notApplicable());
				return;
			}
			setAuthenticationResult(credentialExchange.verifyCode(sentCode,
					answerField.getValue(), subjectOpt.get(), sandboxCallback));
		}

		private void setAuthenticationResult(AuthenticationResult authenticationResult)
		{
			if (authenticationResult.getStatus() == Status.success)
			{
				clear();
				setEnabled(false);
				callback.onCompletedAuthentication(authenticationResult);
			} else if (authenticationResult.getStatus() == Status.unknownRemotePrincipal)
			{
				clear();
				callback.onCompletedAuthentication(authenticationResult);
			} else
			{
				setError();
				usernameField.focus();
				callback.onCompletedAuthentication(authenticationResult);
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
					credEditor, credResetLauncher.getConfiguration());
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

		public void setSandboxCallback(SandboxAuthnResultCallback sandboxCallback)
		{
			this.sandboxCallback = sandboxCallback;
		}

		void setAuthenticatedIdentity(Entity authenticatedIdentity)
		{
			this.presetEntity = authenticatedIdentity;
			sendCodeButton.setVisible(false);
			mainLayout.removeComponent(usernameField);
			mainLayout.removeComponent(resetButton);
			mainLayout.removeComponent(usernameLabel);
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
		private SMSRetrievalComponent theComponent;

		public SMSRetrievalUI(CredentialEditor credEditor)
		{
			this.theComponent = new SMSRetrievalComponent(credEditor);
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
		public Resource getImage()
		{
			return Images.mobile_sms.getResource();
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
