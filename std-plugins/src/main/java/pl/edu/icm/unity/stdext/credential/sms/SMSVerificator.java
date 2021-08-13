/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.stdext.credential.sms;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticatedEntity;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.AuthenticationResult.ResolvableError;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.LocalAuthenticationResult;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalCredentialVerificatorFactory;
import pl.edu.icm.unity.engine.api.authn.local.AbstractLocalVerificator;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.confirmation.SMSCode;
import pl.edu.icm.unity.engine.api.msg.LocaleHelper;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;
import pl.edu.icm.unity.engine.api.utils.PrototypeComponent;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.identity.EmailIdentity;
import pl.edu.icm.unity.stdext.identity.UsernameIdentity;
import pl.edu.icm.unity.types.authn.CredentialPublicInformation;
import pl.edu.icm.unity.types.authn.LocalCredentialState;
import pl.edu.icm.unity.types.basic.EntityParam;

/**
 * Ordinary sms credential verificator.
 * <p>
 * Additionally configuration of the credential may allow for lost phone recovery feature. Then are stored
 * email verification settings and confirmation code length.
 * 
 * @see SMSCredential
 *
 * @author P.Piernik
 */
@PrototypeComponent
public class SMSVerificator extends AbstractLocalVerificator implements SMSExchange 
{ 	
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, SMSVerificator.class);
	
	public static final String NAME = "sms";
	public static final String DESC = "Verifies sms";
	public static final String[] IDENTITY_TYPES = {UsernameIdentity.ID, EmailIdentity.ID};
	
	private SMSCredential credential = new SMSCredential();
	private NotificationProducer notificationProducer;
	private CredentialHelper credentialHelper;
	private AuthnSMSCounter smslimitCache;

	@Autowired
	public SMSVerificator(NotificationProducer notificationProducer,
			CredentialHelper credentialHelper, AuthnSMSCounter smslimitCache)
	{
		super(NAME, DESC, SMSExchange.ID, true);
		this.notificationProducer = notificationProducer;
		this.credentialHelper = credentialHelper;
		this.smslimitCache = smslimitCache;
	}

	@Override
	public String prepareCredential(String rawCredential, 
			String currentCredential, boolean verifyNew)
			throws IllegalCredentialException, InternalException
	{
		return SMSCredentialDBState.toJson(credential, rawCredential,
				System.currentTimeMillis());
	}

	@Override
	public CredentialPublicInformation checkCredentialState(String currentCredential)
			throws InternalException
	{
		SMSCredentialDBState parsedCred = SMSCredentialDBState.fromJson(currentCredential);

		if (parsedCred.getValue() == null || parsedCred.getValue().isEmpty())
			return new CredentialPublicInformation(LocalCredentialState.notSet, "");

		SMSCredentialExtraInfo pei = new SMSCredentialExtraInfo(parsedCred.getTime(),
				parsedCred.getValue());
		String extraInfo = pei.toJson();
		return new CredentialPublicInformation(LocalCredentialState.correct, extraInfo);
	}

	@Override
	public String invalidate(String currentCredential)
	{
		throw new IllegalStateException("This credential doesn't support invalidation");
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		return JsonUtil.serialize(credential.getSerializedConfiguration());
	}

	@Override
	public void setSerializedConfiguration(String json) throws InternalException
	{
		credential.setSerializedConfiguration(JsonUtil.parse(json));
	}

	@Override
	public SMSCode sendCode(AuthenticationSubject subject, boolean force) throws EngineException
	{

		if (isAuthSMSLimitExceeded(subject))
		{
			if (force)
			{
				log.warn("Forcing sending authn sms code to the user " + subject
						+ ", but authn sms limit is exceeded");
			} else
			{
				log.warn("Authn sms limit to the user " + subject
						+ " is exceeded, skipping send authn sms");
				return null;
			}
		}

		smslimitCache.incValue(subject);
		EntityWithCredential resolved = null;
		try
		{
			resolved = identityResolver.resolveSubject(subject, IDENTITY_TYPES,
					credentialName);
		} catch (Exception e)
		{
			log.info("The user for sms authN can not be found: " + subject, e);
			return null;
		}

		String credentialValue = resolved.getCredentialValue();
		if (credentialValue == null)
		{
			log.info("The user {} does not have {} credential defined, skipping sending authentication code",
					subject, credentialName);
			return null;
		}
			

		SMSCredentialDBState credState = SMSCredentialDBState.fromJson(credentialValue);
		String code = CodeGenerator.generateNumberCode(credential.getCodeLength());
		Map<String, String> params = new HashMap<>();
		params.put(SMSAuthnTemplateDef.VAR_CODE, code);
		params.put(SMSAuthnTemplateDef.VAR_USER, identityResolver.getDisplayedUserName(
				new EntityParam(resolved.getEntityId())));
		Locale currentLocale = LocaleHelper.getLocale(null);
		String locale = currentLocale == null ? null : currentLocale.toString();

		notificationProducer.sendNotification(credState.getValue(),
				credential.getMessageTemplate(), params, locale);

		return new SMSCode(
				System.currentTimeMillis()
						+ (credential.getValidityTime() * 60 * 1000),
				code, credState.getValue());
	}

	@Override
	public AuthenticationResult verifyCode(SMSCode sentCode, String codeFromUser, AuthenticationSubject subject)
	{
		return verifyCodeInternal(sentCode, codeFromUser, subject);
	}

	private AuthenticationResult verifyCodeInternal(SMSCode sentCode, String codeFromUser,
			AuthenticationSubject subject)
	{
		if (sentCode == null)
			return getGenericError();

		EntityWithCredential resolved;
		try
		{
			resolved = identityResolver.resolveSubject(subject, IDENTITY_TYPES,
					credentialName);
		} catch (Exception e)
		{
			log.info("The user for sms authN can not be found: " + subject, e);
			return getGenericError();
		}

		if (System.currentTimeMillis() > sentCode.getValidTo())
		{
			log.info("SMS code provided by " + subject + " is invalid");
			return getGenericError();
		}

		if (codeFromUser == null || !sentCode.getValue().equals(codeFromUser))
		{
			log.info("SMS code provided by " + subject + " is incorrect");
			return getGenericError();
		}

		AuthenticatedEntity ae = new AuthenticatedEntity(resolved.getEntityId(), subject,
				null);
		smslimitCache.reset(subject);
		return LocalAuthenticationResult.successful(ae);
	}

	private static AuthenticationResult getGenericError()
	{
		return LocalAuthenticationResult.failed(new ResolvableError("WebSMSRetrieval.wrongCode"));
	}
	
	@Override
	public SMSCredentialResetImpl getSMSCredentialResetBackend()
	{
		return new SMSCredentialResetImpl(notificationProducer, identityResolver, this,
				credentialHelper, credentialName,
				credential.getSerializedConfiguration(),
				credential.getRecoverySettings());
	}


	@Override
	public boolean isAuthSMSLimitExceeded(AuthenticationSubject username)
	{
		return smslimitCache.getValue(username) >= credential.getAuthnSMSLimit();
	}
	
	@Override
	public boolean isCredentialSet(EntityParam entity) throws EngineException
	{
		return credentialHelper.isCredentialSet(entity, credentialName);
	}
	

	@Override
	public boolean isCredentialDefinitionChagneOutdatingCredentials(String newCredentialDefinition)
	{
		return false;
	}
	
	@Component
	public static class Factory extends AbstractLocalCredentialVerificatorFactory
	{
		@Autowired
		public Factory(ObjectFactory<SMSVerificator> factory)
		{
			super(NAME, DESC, false, factory);
		}
	}
}
