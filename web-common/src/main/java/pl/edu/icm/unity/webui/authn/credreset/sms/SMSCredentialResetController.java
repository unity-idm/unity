/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import java.util.Optional;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.message.MessageSource;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.webui.authn.CredentialResetLauncher;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFinalMessage;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetFlowConfig;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetScreen;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable;
import pl.edu.icm.unity.webui.authn.credreset.CredentialResetStateVariable.ResetPrerequisite;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.credentials.CredentialEditor;

/**
 * Entry point and controller of the SMS reset flow. Oversees changes of various UI steps in the flow.
 * 
 * @author K. Benedyczak
 */
public class SMSCredentialResetController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, SMSCredentialResetController.class);
	
	private MessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	private Runnable finishHandler;
	private SMSCredentialRecoverySettings settings;
	private CredentialResetScreen mainWrapper;
	private CredentialResetFlowConfig credResetUIConfig;

	private Optional<AuthenticationSubject> presetEntity;
	
	public SMSCredentialResetController(MessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, CredentialResetLauncher.CredentialResetUIConfig config)
	{
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.finishHandler = config.finishCallback;
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
		
		CredentialResetStateVariable.record(ResetPrerequisite.CAPTCHA_PROVIDED);

		gotoVerifyEmailCode();
	}
	
	private void onCodeConfirmedByEmail(String code) throws WrongArgumentException, TooManyAttempts
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED);
		
		backend.verifyDynamicData(code);
		
		CredentialResetStateVariable.record(ResetPrerequisite.CODE_PROVIDED);
		gotoNewMobileCollection();
	}

	protected void onNewCredentialProvided(String updatedValue) throws EngineException
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED, 
				ResetPrerequisite.CODE_PROVIDED);
		
		backend.updateCredential(updatedValue);
		CredentialResetStateVariable.reset();
		
		mainWrapper.setContents(new CredentialResetFinalMessage(credResetUIConfig, 
				msg.getMessage("CredentialReset.successMobile")));
	}
	

	private void gotoVerifyEmailCode()
	{
		if (!sendCodeViaEmail())
			return;
		SMSResetStep2EmailCode emailCodeUI = new SMSResetStep2EmailCode(credResetUIConfig, 
				this::onCodeConfirmedByEmail, this::resendCodeViaEmail);
		mainWrapper.setContents(emailCodeUI);
	}
	
	private void gotoNewMobileCollection()
	{
		SMSResetStep3NewMobile newMobileUI = new SMSResetStep3NewMobile(credResetUIConfig,
				credEditor, this::onNewCredentialProvided, 
				backend.getCredentialConfiguration(), backend.getEntityId());
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
			NotificationPopup.showError(msg.getMessage("error"),
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
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			onCancel();
		}
	}
	
	@FunctionalInterface
	public static interface CodeConsumer 
	{
		void acceptCode(String code) throws TooManyAttempts, WrongArgumentException;
	}

	@FunctionalInterface
	public static interface NewCredentialConsumer 
	{
		void acceptNewCredential(String credential) throws EngineException;
	}
	
	@FunctionalInterface
	public static interface CodeSender 
	{
		void resendCode() throws TooManyAttempts;
	}
}
