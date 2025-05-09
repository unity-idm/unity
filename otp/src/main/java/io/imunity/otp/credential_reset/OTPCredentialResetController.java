/*
 * Copyright (c) 2021 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.otp.credential_reset;

import com.vaadin.flow.component.Component;
import io.imunity.otp.OTPResetSettings;
import io.imunity.otp.OTPResetSettings.ConfirmationMode;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFinalMessage;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetScreen;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetStateVariable;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetStateVariable.ResetPrerequisite;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;

import java.util.Optional;

/**
 * Entry point and controller of the OTP reset flow. Oversees changes of various UI steps in the flow.
 */
public class OTPCredentialResetController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, OTPCredentialResetController.class);
	
	enum VerificationMethod {EMAIL, MOBILE}
	
	private final MessageSource msg;
	private final OTPCredentialReset backend;
	private final CredentialEditor credEditor;
	private final Runnable finishHandler;
	private final OTPResetSettings settings;
	private final CredentialResetFlowConfig credResetUIConfig;
	private final NotificationPresenter notificationPresenter;

	private CredentialResetScreen mainWrapper;
	private Optional<AuthenticationSubject> presetEntity;
	
	public OTPCredentialResetController(MessageSource msg, OTPCredentialReset backend,
	                                    CredentialEditor credEditor, CredentialResetLauncher.CredentialResetUIConfig uiConfig,
	                                    NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.backend = backend;
		this.settings = backend.getResetSettings();
		this.credEditor = credEditor;
		this.finishHandler = uiConfig.finishCallback;
		this.notificationPresenter = notificationPresenter;
		credResetUIConfig = new CredentialResetFlowConfig(uiConfig.logo, msg, this::onCancel,
				uiConfig.infoWidth, uiConfig.contentsWidth, uiConfig.compactLayout);
	}

	public Component getInitialUI(Optional<AuthenticationSubject> presetEntity)
	{
		this.presetEntity = presetEntity;
		CredentialResetStateVariable.reset();
		mainWrapper = new CredentialResetScreen();
		mainWrapper.setContents(new OTPResetStep1Captcha(credResetUIConfig, presetEntity.isEmpty(), this::onUsernameCollected));
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
		
		CredentialResetStateVariable.record(ResetPrerequisite.CAPTCHA_PROVIDED);
		CredentialResetStateVariable.record(ResetPrerequisite.STATIC_CHECK_PASSED);
		
		if (settings.confirmationMode.requiresEmailConfirmation())
		{
			gotoVerifyEmailCode();
		} else if (settings.confirmationMode.requiresMobileConfirmation())
		{
			gotoVerifyMobileCode();
		} else
		{
			gotoVerificationMethodChoice();
		}
	}
	
	private void onConfirmationModeSelected(VerificationMethod method)
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED,
				ResetPrerequisite.STATIC_CHECK_PASSED);
		
		if (method == VerificationMethod.EMAIL)
		{
			gotoVerifyEmailCode();
		} else
		{
			gotoVerifyMobileCode();
		}	
	}
	
	private void onCodeConfirmedByEmail(String code) throws WrongArgumentException, TooManyAttempts
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED,
				ResetPrerequisite.STATIC_CHECK_PASSED);
		
		backend.verifyDynamicData(code);
		
		if (settings.confirmationMode == ConfirmationMode.EMAIL_AND_MOBILE)
		{
			gotoVerifyMobileCode();
		} else
		{
			CredentialResetStateVariable.record(ResetPrerequisite.CODE_PROVIDED);
			gotoNewPasswordCollection();
		}
	}

	private void onCodeConfirmedByMobile(String code) throws WrongArgumentException, TooManyAttempts
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED,
				ResetPrerequisite.STATIC_CHECK_PASSED);
		backend.verifyDynamicData(code);
		CredentialResetStateVariable.record(ResetPrerequisite.CODE_PROVIDED);
		gotoNewPasswordCollection();
	}
	
	protected void onNewCredentialProvided(String updatedValue) throws EngineException
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED,
				ResetPrerequisite.STATIC_CHECK_PASSED, ResetPrerequisite.CODE_PROVIDED);
		
		backend.updateCredential(updatedValue);
		CredentialResetStateVariable.reset();
		mainWrapper.removeAll();
		mainWrapper.setContents(new CredentialResetFinalMessage(credResetUIConfig,
				msg.getMessage("OTPCredentialReset.success")));
	}
	

	private void gotoVerificationMethodChoice()
	{
		OTPResetStep2VerificationChoice methodChoiceUI = new OTPResetStep2VerificationChoice(
				credResetUIConfig, this::onConfirmationModeSelected);
		mainWrapper.removeAll();
		mainWrapper.setContents(methodChoiceUI);
	}
	
	private void gotoVerifyEmailCode()
	{
		if (!sendCodeViaEmail())
			return;
		OTPResetStep3EmailCode emailCodeUI = new OTPResetStep3EmailCode(
				credResetUIConfig, this::onCodeConfirmedByEmail, this::resendCodeViaEmail, notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(emailCodeUI);
	}
	
	private void gotoVerifyMobileCode()
	{
		if (!sendCodeViaMobile())
			return;
		OTPResetStep3MobileCode mobileCodeUI = new OTPResetStep3MobileCode(
				credResetUIConfig, this::onCodeConfirmedByMobile, this::resendCodeViaMobile, notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(mobileCodeUI);
	}

	private void gotoNewPasswordCollection()
	{
		OTPResetStep4NewCredential newCredential = new OTPResetStep4NewCredential(credResetUIConfig,
				credEditor, this::onNewCredentialProvided, 
				backend.getCredentialConfiguration(), backend.getEntityId(), notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(newCredential);
	}
	
	private boolean sendCodeViaEmail()
	{
		try
		{
			backend.sendCode(settings.emailSecurityCodeMsgTemplate, false);
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
			backend.sendCode(settings.emailSecurityCodeMsgTemplate, false);
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
			backend.sendCode(settings.mobileSecurityCodeMsgTemplate, true);
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
			backend.sendCode(settings.mobileSecurityCodeMsgTemplate, true);
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
