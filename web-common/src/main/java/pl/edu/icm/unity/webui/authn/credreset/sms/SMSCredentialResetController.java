/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.sms;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.sms.SMSCredentialRecoverySettings;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.basic.IdentityTaV;
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
	
	private UnityMessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	private Runnable finishHandler;
	private SMSCredentialRecoverySettings settings;
	private CredentialResetScreen mainWrapper;
	private CredentialResetFlowConfig credResetUIConfig;
	
	public SMSCredentialResetController(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, CredentialResetLauncher.CredentialResetUIConfig config)
	{
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.finishHandler = config.finishCallback;
		credResetUIConfig = new CredentialResetFlowConfig(config.logo, msg, this::onCancel, 
				config.infoWidth, config.contentsWidth, config.compactLayout);
	}

	public Component getInitialUI()
	{
		CredentialResetStateVariable.reset();
		this.settings = new SMSCredentialRecoverySettings(JsonUtil.parse(backend.getSettings()));
		mainWrapper = new CredentialResetScreen();
		mainWrapper.setContents(new SMSResetStep1Captcha(credResetUIConfig, settings.isCapchaRequired(),
				this::onUsernameCollected));
		return mainWrapper;
	}
	
	private void onCancel()
	{
		CredentialResetStateVariable.reset();
		finishHandler.run();
	}
	
	private void onUsernameCollected(String username)
	{
		backend.setSubject(new IdentityTaV(UsernameIdentity.ID, username));
		
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
			log.debug("Credential reset notification failed", e);
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
			log.debug("Credential reset notification failed", e);
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
