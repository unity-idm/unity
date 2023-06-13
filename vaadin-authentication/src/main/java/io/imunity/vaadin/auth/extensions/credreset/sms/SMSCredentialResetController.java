/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package io.imunity.vaadin.auth.extensions.credreset.sms;

import com.vaadin.flow.component.Component;
import io.imunity.vaadin.auth.CredentialResetLauncher;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFinalMessage;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetFlowConfig;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetScreen;
import io.imunity.vaadin.auth.extensions.credreset.CredentialResetStateVariable;
import io.imunity.vaadin.elements.NotificationPresenter;
import io.imunity.vaadin.endpoint.common.plugins.credentials.CredentialEditor;
import org.apache.logging.log4j.Logger;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;

import java.util.Optional;

/**
 * Entry point and controller of the SMS reset flow. Oversees changes of various UI steps in the flow.
 */
public class SMSCredentialResetController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SMSCredentialResetController.class);
	
	private final MessageSource msg;
	private final CredentialReset backend;
	private final CredentialEditor credEditor;
	private final Runnable finishHandler;
	private SMSCredentialRecoverySettings settings;
	private CredentialResetScreen mainWrapper;
	private final CredentialResetFlowConfig credResetUIConfig;
	private final NotificationPresenter notificationPresenter;

	private Optional<AuthenticationSubject> presetEntity;
	
	public SMSCredentialResetController(MessageSource msg, CredentialReset backend,
	                                    CredentialEditor credEditor, CredentialResetLauncher.CredentialResetUIConfig config,
	                                    NotificationPresenter notificationPresenter)
	{
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.finishHandler = config.finishCallback;
		this.notificationPresenter = notificationPresenter;
		credResetUIConfig = new CredentialResetFlowConfig(config.logo, msg, this::onCancel,
				config.infoWidth, config.contentsWidth, config.compactLayout);
	}

	public Component getInitialUI(Optional<AuthenticationSubject> presetEntity)
	{
		this.presetEntity = presetEntity;
		CredentialResetStateVariable.reset();
		this.settings = new SMSCredentialRecoverySettings(JsonUtil.parse(backend.getSettings()));
		mainWrapper = new CredentialResetScreen();
		mainWrapper.setContents(new SMSResetStep1Captcha(credResetUIConfig, settings.isCapchaRequire(),
				this::onUsernameCollected, !presetEntity.isPresent()));
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
		
		CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED);

		gotoVerifyEmailCode();
	}
	
	private void onCodeConfirmedByEmail(String code) throws WrongArgumentException, TooManyAttempts
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED);
		
		backend.verifyDynamicData(code);
		
		CredentialResetStateVariable.record(CredentialResetStateVariable.ResetPrerequisite.CODE_PROVIDED);
		gotoNewMobileCollection();
	}

	protected void onNewCredentialProvided(String updatedValue) throws EngineException
	{
		CredentialResetStateVariable.assertFullfilled(CredentialResetStateVariable.ResetPrerequisite.CAPTCHA_PROVIDED,
				CredentialResetStateVariable.ResetPrerequisite.CODE_PROVIDED);
		
		backend.updateCredential(updatedValue);
		CredentialResetStateVariable.reset();

		mainWrapper.removeAll();
		mainWrapper.setContents(new CredentialResetFinalMessage(credResetUIConfig,
				msg.getMessage("CredentialReset.successMobile")));
	}
	

	private void gotoVerifyEmailCode()
	{
		if (!sendCodeViaEmail())
			return;
		SMSResetStep2EmailCode emailCodeUI = new SMSResetStep2EmailCode(credResetUIConfig,
				this::onCodeConfirmedByEmail, this::resendCodeViaEmail, notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(emailCodeUI);
	}
	
	private void gotoNewMobileCollection()
	{
		SMSResetStep3NewMobile newMobileUI = new SMSResetStep3NewMobile(credResetUIConfig,
				credEditor, this::onNewCredentialProvided, 
				backend.getCredentialConfiguration(), backend.getEntityId(), notificationPresenter);
		mainWrapper.removeAll();
		mainWrapper.setContents(newMobileUI);
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
}
