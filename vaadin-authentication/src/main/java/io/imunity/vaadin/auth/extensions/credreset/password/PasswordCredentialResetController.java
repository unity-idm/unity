/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset.password;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFinalMessage;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetScreen;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetStateVariable;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import org.apache.logging.log4j.Logger;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;

import java.util.Optional;

import static io.imunity.vaadin.auth.extensions.credreset.CredentialResetStateVariable.ResetPrerequisite.STATIC_CHECK_PASSED;

/**
 * Entry point and controller of the password reset flow. Oversees changes of various UI steps in the flow.
 */
public class PasswordCredentialResetController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordCredentialResetController.class);
	public enum VerificationMethod {Email, Mobile}
	
	private final MessageSource msg;
	private final CredentialReset backend;
	private final CredentialEditor credEditor;
	private final Runnable finishHandler;
	private PasswordCredentialResetSettings settings;
	private CredentialResetScreen mainWrapper;
	private final CredentialResetFlowConfig credResetUIConfig;
	private Optional<AuthenticationSubject> presetEntity;
	private NotificationPresenter notificationPresenter;

	public PasswordCredentialResetController(MessageSource msg, CredentialReset backend,
	                                         CredentialEditor credEditor, CredentialResetLauncher.CredentialResetUIConfig credResetConfig,
	                                         NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.notificationPresenter = notificationPresenter;
		this.finishHandler = credResetConfig.finishCallback;
		credResetUIConfig = new CredentialResetFlowConfig(credResetConfig.logo, msg, this::onCancel,
				credResetConfig.infoWidth, credResetConfig.contentsWidth, credResetConfig.compactLayout);
	}

	public Component getInitialUI(Optional<AuthenticationSubject> presetEntity)
	{
		this.presetEntity = presetEntity;
		CredentialResetStateVariable.reset();
		mainWrapper = new CredentialResetScreen();
		mainWrapper.setContents(new PasswordResetStep1Captcha(credResetUIConfig, this::onUsernameCollected));
		return mainWrapper;
	}
	
	private void onCancel()
	{
		CredentialResetStateVariable.reset();
		finishHandler.run();
	}
	
	private void onUsernameCollected(String username)
	{
		AuthenticationSubject subject = presetEntity.orElse(AuthenticationSubject.identityBased(username));
		backend.setSubject(subject);
		this.settings = new PasswordCredentialResetSettings(JsonUtil.parse(backend.getSettings()));
		
		PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(JsonUtil.parse(backend.getSettings()));
		CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED);
		
		if (settings.isRequireSecurityQuestion())
		{
			PasswordResetStep2Question securityQuestionUI = new PasswordResetStep2Question(credResetUIConfig,
					backend.getSecurityQuestion(),  
					this::onSecurityAnswerCollected,
					notificationPresenter);
			mainWrapper.removeAll();
			mainWrapper.setContents(securityQuestionUI);
		} else 
		{
			CredentialResetStateVariable.record(STATIC_CHECK_PASSED);
			if (settings.isRequireEmailConfirmation())
			{
				gotoVerifyEmailCode();
			} else if (settings.isRequireMobileConfirmation())
			{
				gotoVerifyMobileCode();
			} else
			{
				gotoVerificationMethodChoice();
			}
		}
	}
	
	private void onSecurityAnswerCollected(String answer) throws TooManyAttempts, WrongArgumentException, IllegalIdentityValueException
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED);

		backend.verifyStaticData(answer);
		
		CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.STATIC_CHECK_PASSED);
		
		if (settings.isRequireEmailConfirmation())
		{
			gotoVerifyEmailCode();
		} else if (settings.isRequireMobileConfirmation())
		{
			gotoVerifyMobileCode();
		} else if (settings.getConfirmationMode().equals(ConfirmationMode.RequireEmailOrMobile))
		{
			gotoVerificationMethodChoice();
		} else
		{
			CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.CODE_PROVIDED);
			gotoNewPasswordCollection();
		}
	}
	
	private void onConfirmationModeSelected(VerificationMethod method)
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED,
				STATIC_CHECK_PASSED);
		
		if (method.equals(VerificationMethod.Email))
		{
			gotoVerifyEmailCode();
		} else
		{
			gotoVerifyMobileCode();
		}	
	}
	
	private void onCodeConfirmedByEmail(String code) throws WrongArgumentException, TooManyAttempts
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED,
				CredentialResetStateVariable.ResetPrerequisite.STATIC_CHECK_PASSED);
		
		backend.verifyDynamicData(code);
		
		if (settings.isRequireMobileConfirmation())
		{
			gotoVerifyMobileCode();
		} else
		{
			CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.CODE_PROVIDED);
			gotoNewPasswordCollection();
		}
	}

	private void onCodeConfirmedByMobile(String code) throws WrongArgumentException, TooManyAttempts
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED,
				CredentialResetStateVariable.ResetPrerequisite.STATIC_CHECK_PASSED);
		backend.verifyDynamicData(code);
		CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.CODE_PROVIDED);
		gotoNewPasswordCollection();
	}
	
	protected void onNewCredentialProvided(String updatedValue) throws EngineException
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED,
				CredentialResetStateVariable.ResetPrerequisite.STATIC_CHECK_PASSED, CredentialResetStateVariable.ResetPrerequisite.CODE_PROVIDED);
		
		backend.updateCredential(updatedValue);
		CredentialResetStateVariable.reset();
		mainWrapper.removeAll();
		mainWrapper.setContents(new CredentialResetFinalMessage(credResetUIConfig,
				msg.getMessage("CredentialReset.successPassword")));
	}
	

	private void gotoVerificationMethodChoice()
	{
		PasswordResetStep3VerificationChoice methodChoiceUI = new PasswordResetStep3VerificationChoice(
				credResetUIConfig, this::onConfirmationModeSelected);
		mainWrapper.removeAll();
		mainWrapper.setContents(methodChoiceUI);
	}
	
	private void gotoVerifyEmailCode()
	{
		if (!sendCodeViaEmail())
			return;
		PasswordResetStep4EmailCode emailCodeUI = new PasswordResetStep4EmailCode(
				credResetUIConfig, this::onCodeConfirmedByEmail, this::resendCodeViaEmail, notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(emailCodeUI);
	}
	
	private void gotoVerifyMobileCode()
	{
		if (!sendCodeViaMobile())
			return;
		PasswordResetStep4MobileCode mobileCodeUI = new PasswordResetStep4MobileCode(
				credResetUIConfig, this::onCodeConfirmedByMobile, this::resendCodeViaMobile, notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(mobileCodeUI);
	}

	private void gotoNewPasswordCollection()
	{
		PasswordResetStep5NewPassword newPassUI = new PasswordResetStep5NewPassword(credResetUIConfig,
				credEditor, this::onNewCredentialProvided, 
				backend.getCredentialConfiguration(), backend.getEntityId(), notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(newPassUI);
	}
	
	private boolean sendCodeViaEmail()
	{
		try
		{
			backend.sendCode(settings.getEmailSecurityCodeMsgTemplate(), false);
			return true;
		} catch (Exception e)
		{
			log.warn("Credential reset notification failed", e);
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			onCancel();
			return false;
		}
	}
	
	private void resendCodeViaEmail() throws TooManyAttempts
	{
		try
		{
			backend.sendCode(settings.getEmailSecurityCodeMsgTemplate(), false);
		} catch (TooManyAttempts e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Credential reset notification failed", e);
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			onCancel();
		}
	}
	
	private boolean sendCodeViaMobile()
	{
		try
		{
			backend.sendCode(settings.getMobileSecurityCodeMsgTemplate(), true);
			return true;
		} catch (Exception e)
		{
			log.warn("Credential reset notification failed", e);
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			onCancel();
			return false;
		}
	}
	
	private void resendCodeViaMobile() throws TooManyAttempts
	{
		try
		{
			backend.sendCode(settings.getMobileSecurityCodeMsgTemplate(), true);
		} catch (TooManyAttempts e)
		{
			throw e;
		} catch (Exception e)
		{
			log.warn("Credential reset notification failed", e);
			notificationPresenter.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			onCancel();
		}
	}
}
