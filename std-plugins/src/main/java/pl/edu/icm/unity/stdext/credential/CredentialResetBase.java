/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.exceptions.WrongArgumentException;
import pl.edu.icm.unity.base.identity.EntityParam;
import pl.edu.icm.unity.base.identity.IllegalIdentityValueException;
import pl.edu.icm.unity.base.utils.JsonUtil;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.authn.AuthenticationSubject;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.EntityWithCredential;
import pl.edu.icm.unity.engine.api.authn.TooManyAttempts;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.msg.LocaleHelper;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.engine.api.utils.CodeGenerator;

/**
 * Base for credential reset implementation of {@link CredentialReset}. This implementation is stateful, i.e. from creation it
 * must be used exclusively by a single reset procedure.
 * @author P. Piernik
 */
public abstract class CredentialResetBase implements CredentialReset
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_AUTHN, CredentialResetBase.class);
	protected static final int MAX_ANSWER_ATTEMPTS = 2;
	private static final int MAX_RESENDS = 3;
	public static final Duration DEFAULT_MAX_CODE_VALIDITY = Duration.ofMinutes(30);
	private Duration maxCodeValidity;
	
	private NotificationProducer notificationProducer;
	private IdentityResolver identityResolver;
	private CredentialHelper credentialHelper;
	private LocalCredentialVerificator localCredentialHandler;
	
	protected EntityWithCredential resolved;

	private String credentialId;
	private ObjectNode completeCredentialConfiguration;
	
	private String codeSent;
	private LocalDateTime codeValidityEnd;
	private int dynamicAnswerAttempts = 0;
	private int codeSendingAttempts = 0;
	private AuthenticationSubject requestedSubject;
	
	public CredentialResetBase(NotificationProducer notificationProducer,
			IdentityResolver identityResolver,
			LocalCredentialVerificator localVerificator,
			CredentialHelper credentialHelper,
			String credentialId, 
			ObjectNode completeCredentialConfiguration,
			Duration maxCodeValidity)
	{
		this.notificationProducer = notificationProducer;
		this.credentialHelper = credentialHelper;
		this.identityResolver = identityResolver;
		this.credentialId = credentialId;
		this.localCredentialHandler = localVerificator;
		this.completeCredentialConfiguration = completeCredentialConfiguration;
		this.maxCodeValidity = maxCodeValidity;
	}

	public void setSubject(AuthenticationSubject subject, String[] idTypes)
	{
		this.requestedSubject = subject;
		try
		{
			resolved = identityResolver.resolveSubject(subject, idTypes, credentialId);
		} catch (IllegalIdentityValueException e)
		{
			//OK - can happen, we can ignore
		} catch (Exception e)
		{
			log.error("Exception when trying to resolve identity", e);
		}
		
	}
	
	protected boolean checkSubject()
	{
		return resolved != null && resolved.getCredentialValue() != null;
	}
	
	public Long getEntityId()
	{
		return resolved.getEntityId();
	}
	
	
	@Override
	public String getSettings()
	{
		return getCredentialSettings();
	}

	protected abstract String getCredentialSettings();

	protected AuthenticationSubject getRequestedSubject()
	{
		return requestedSubject;
	}
	
	@Override
	public String getSecurityQuestion()
	{
		return null;
	}
	
	@Override
	public void verifyStaticData(String answer) throws WrongArgumentException,
		IllegalIdentityValueException, TooManyAttempts
	{
		//ok
	}
	
	private void createCode(boolean onlyNumberCode)
	{
		int codeLen = getCodeLength();
		if (!onlyNumberCode)
		{
			codeSent = CodeGenerator.generateMixedCharCode(codeLen);
		} else
		{
			codeSent = CodeGenerator.generateNumberCode(codeLen);
		}
		codeValidityEnd = LocalDateTime.now().plus(maxCodeValidity);
	}
	
	protected abstract int getCodeLength();

	@Override
	public void sendCode(String msgTemplate, boolean onlyNumberCode) throws EngineException
	{
		if (!checkSubject())
			throw new IllegalIdentityValueException("Identity was not resolved or has no credential set");
		if (codeSendingAttempts >= MAX_RESENDS)
			throw new TooManyAttempts();
		codeSendingAttempts++;
		if (codeSent == null)
			createCode(onlyNumberCode);

		String username = identityResolver.getDisplayedUserName(new EntityParam(resolved.getEntityId()));
		Map<String, String> params = new HashMap<>();
		params.put(CredentialResetTemplateDefBase.VAR_CODE, codeSent);
		params.put(CredentialResetTemplateDefBase.VAR_USER, username);
		Locale currentLocale = LocaleHelper.getLocale(null);
		String locale = currentLocale == null ? null : currentLocale.toString();
		notificationProducer.sendNotification(new EntityParam(resolved.getEntityId()), 
				msgTemplate, params, locale, username, true);
	}
	
	public String getSentCode()
	{
		return codeSent;
	}
	
	@Override
	public void verifyDynamicData(String answer) throws WrongArgumentException, TooManyAttempts
	{
		if (dynamicAnswerAttempts >= MAX_ANSWER_ATTEMPTS)
			throw new TooManyAttempts();
		dynamicAnswerAttempts++;
		if (LocalDateTime.now().isAfter(codeValidityEnd))
			throw new TooManyAttempts();
		if (codeSent == null || !codeSent.equals(answer))
			throw new WrongArgumentException("The code is invalid");
	
		dynamicAnswerAttempts = 0;
		codeSendingAttempts = 0;
		codeSent = null;
	}
	
	@Override
	public String getCredentialConfiguration()
	{
		return JsonUtil.toJsonString(completeCredentialConfiguration);
	}
	
	@Override
	public void updateCredential(String newCredential) throws EngineException
	{
		if (!checkSubject())
			throw new IllegalStateException("Identity was not resolved.");

		credentialHelper.setCredential(resolved.getEntityId(), credentialId, newCredential, 
				localCredentialHandler);
	}
}
