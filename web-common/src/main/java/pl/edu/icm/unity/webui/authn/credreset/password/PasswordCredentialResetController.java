/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.authn.credreset.password;

import org.apache.logging.log4j.Logger;

import com.vaadin.ui.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings;
import pl.edu.icm.unity.stdext.credential.pass.PasswordCredentialResetSettings.ConfirmationMode;
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
 * Entry point and controller of the password reset flow. Oversees changes of various UI steps in the flow.
 * 
 * @author K. Benedyczak
 */
public class PasswordCredentialResetController
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_WEB, PasswordCredentialResetController.class);
	public enum VerificationMethod {Email, Mobile}
	
	private UnityMessageSource msg;
	private CredentialReset backend;
	private CredentialEditor credEditor;
	private Runnable finishHandler;
	private PasswordCredentialResetSettings settings;
	private CredentialResetScreen mainWrapper;
	private CredentialResetFlowConfig credResetUIConfig;
	
	public PasswordCredentialResetController(UnityMessageSource msg, CredentialReset backend,
			CredentialEditor credEditor, CredentialResetLauncher.CredentialResetUIConfig credResetConfig)
	{
		this.msg = msg;
		this.backend = backend;
		this.credEditor = credEditor;
		this.finishHandler = credResetConfig.finishCallback;
		credResetUIConfig = new CredentialResetFlowConfig(credResetConfig.logo, msg, this::onCancel, 
				credResetConfig.infoWidth, credResetConfig.contentsWidth, credResetConfig.compactLayout);
	}

	public Component getInitialUI()
	{
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
		backend.setSubject(new IdentityTaV(UsernameIdentity.ID, username));
		this.settings = new PasswordCredentialResetSettings(JsonUtil.parse(backend.getSettings()));
		
		PasswordCredentialResetSettings settings = new PasswordCredentialResetSettings(JsonUtil.parse(backend.getSettings()));
		CredentialResetStateVariable.record(ResetPrerequisite.CAPTCHA_PROVIDED);
		
		if (settings.isRequireSecurityQuestion())
		{
			PasswordResetStep2Question securityQuestionUI = new PasswordResetStep2Question(credResetUIConfig,
					backend.getSecurityQuestion(), username, 
					this::onSecurityAnswerCollected);
			mainWrapper.setContents(securityQuestionUI);
		} else 
		{
			CredentialResetStateVariable.record(ResetPrerequisite.STATIC_CHECK_PASSED);
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
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED);

		backend.verifyStaticData(answer);
		
		CredentialResetStateVariable.record(ResetPrerequisite.STATIC_CHECK_PASSED);
		
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
			CredentialResetStateVariable.record(ResetPrerequisite.CODE_PROVIDED);
			gotoNewPasswordCollection();
		}
	}
	
	private void onConfirmationModeSelected(VerificationMethod method)
	{
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED,
				ResetPrerequisite.STATIC_CHECK_PASSED);
		
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
		CredentialResetStateVariable.assertFullfilled(ResetPrerequisite.CAPTCHA_PROVIDED,
				ResetPrerequisite.STATIC_CHECK_PASSED);
		
		backend.verifyDynamicData(code);
		
		if (settings.isRequireMobileConfirmation())
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
		mainWrapper.setContents(new CredentialResetFinalMessage(credResetUIConfig,
				msg.getMessage("CredentialReset.successPassword")));
	}
	

	private void gotoVerificationMethodChoice()
	{
		PasswordResetStep3VerificationChoice methodChoiceUI = new PasswordResetStep3VerificationChoice(
				credResetUIConfig, this::onConfirmationModeSelected);
		mainWrapper.setContents(methodChoiceUI);
	}
	
	private void gotoVerifyEmailCode()
	{
		if (!sendCodeViaEmail())
			return;
		PasswordResetStep4EmailCode emailCodeUI = new PasswordResetStep4EmailCode(
				credResetUIConfig, this::onCodeConfirmedByEmail, this::resendCodeViaEmail);
		mainWrapper.setContents(emailCodeUI);
	}
	
	private void gotoVerifyMobileCode()
	{
		if (!sendCodeViaMobile())
			return;
		PasswordResetStep4MobileCode mobileCodeUI = new PasswordResetStep4MobileCode(
				credResetUIConfig, this::onCodeConfirmedByMobile, this::resendCodeViaMobile);
		mainWrapper.setContents(mobileCodeUI);
	}

	private void gotoNewPasswordCollection()
	{
		PasswordResetStep5NewPassword newPassUI = new PasswordResetStep5NewPassword(credResetUIConfig,
				credEditor, this::onNewCredentialProvided, 
				backend.getCredentialConfiguration(), backend.getEntityId());
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
	
	private boolean sendCodeViaMobile()
	{
		try
		{
			backend.sendCode(settings.getMobileSecurityCodeMsgTemplate(), true);
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
			log.debug("Credential reset notification failed", e);
			NotificationPopup.showError(msg.getMessage("error"),
					msg.getMessage("CredentialReset.resetNotPossible"));
			onCancel();
		}
	}
	
	@FunctionalInterface
	public static interface AnswerConsumer 
	{
		void acceptAnswer(String answer) throws TooManyAttempts, WrongArgumentException, IllegalIdentityValueException;
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
