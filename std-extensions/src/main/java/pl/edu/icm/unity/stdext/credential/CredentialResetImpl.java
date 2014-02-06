/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.log4j.Logger;
import org.bouncycastle.util.Arrays;

import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.notifications.NotificationProducer;
import pl.edu.icm.unity.server.api.internal.IdentityResolver;
import pl.edu.icm.unity.server.authn.CredentialHelper;
import pl.edu.icm.unity.server.authn.CredentialReset;
import pl.edu.icm.unity.server.authn.CredentialResetSettings;
import pl.edu.icm.unity.server.authn.EntityWithCredential;
import pl.edu.icm.unity.server.authn.LocalCredentialVerificator;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import pl.edu.icm.unity.stdext.utils.CryptoUtils;
import pl.edu.icm.unity.types.basic.EntityParam;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Default implementation of {@link CredentialReset}. This implementation is stateful, i.e. from creation it
 * must be used exclusively by a single reset procedure.
 * @author K. Benedyczak
 */
public class CredentialResetImpl implements CredentialReset
{
	private static final Logger log = Log.getLogger(Log.U_SERVER, CredentialResetImpl.class);
	private static final int MAX_ANSWER_ATTEMPTS = 2;
	private static final int MAX_RESENDS = 3;
	private static final long MAX_CODE_VALIDITY = 30*3600;
	
	public static final String PASSWORD_RESET_TPL = "passwordResetCode";
	public static final String CODE_VAR = "code";
	public static final String USER_VAR = "username";
	
	private static final char[] CHARS_POOL = {'Q', 'W', 'E', 'R', 'T', 'Y', 'U', 'I', 'O', 'P', 
			'A', 'S', 'D', 'F', 'G', 'H', 'J', 'K', 'L',
			'Z', 'X', 'C', 'V', 'B', 'N', 'M', 
			'1', '2', '3', '4', '5', '6', '7', '8', '9', '0'};
	
	private NotificationProducer notificationProducer;
	private IdentityResolver identityResolver;
	private CredentialHelper credentialHelper;
	private LocalCredentialVerificator localCredentialHandler;
	
	private EntityParam subject;
	private EntityWithCredential resolved;
	private PasswordCredentialDBState credState;
	
	private String credentialId;
	private String completeCredentialConfiguration;
	private CredentialResetSettings settings;
	private String codeSent;
	private long codeValidityEnd;
	private int answerAttempts = 0;
	private int dynamicAnswerAttempts = 0;
	private int codeSendingAttempts = 0;
	private static final Random rnd = new SecureRandom();
	
	public CredentialResetImpl(NotificationProducer notificationProducer,
			IdentityResolver identityResolver,
			LocalCredentialVerificator localVerificator,
			CredentialHelper credentialHelper,
			String credentialId, String completeCredentialConfiguration,
			CredentialResetSettings settings)
	{
		this.notificationProducer = notificationProducer;
		this.credentialHelper = credentialHelper;
		this.identityResolver = identityResolver;
		this.credentialId = credentialId;
		this.localCredentialHandler = localVerificator;
		this.completeCredentialConfiguration = completeCredentialConfiguration;
		this.settings = settings;
	}

	@Override
	public void setSubject(IdentityTaV subject)
	{
		this.subject = new EntityParam(subject);
		try
		{
			resolved = identityResolver.resolveIdentity(subject.getValue(), 
					PasswordVerificator.IDENTITY_TYPES, credentialId);
			String dbCredential = resolved.getCredentialValue();
			credState = PasswordCredentialDBState.fromJson(dbCredential);
		} catch(IllegalIdentityValueException e)
		{
			//OK - can happen, we can ignore
		} catch (Exception e)
		{
			log.error("Exception when trying to resolve identity", e);
		}
		
	}
	
	@Override
	public CredentialResetSettings getSettings()
	{
		return settings;
	}

	@Override
	public String getSecurityQuestion()
	{
		if (credState == null)
			return getFakeQuestion();
		String q = credState.getSecurityQuestion();
		if (q == null)
			return getFakeQuestion();
		return q;
	}

	private String getFakeQuestion()
	{
		List<String> questions = settings.getQuestions();
		int hash = subject.getIdentity().getValue().hashCode();
		int num = (hash < 0 ? -hash : hash) % questions.size();
		return questions.get(num);
	}
	
	@Override
	public void verifyStaticData(String answer) throws WrongArgumentException,
		IllegalIdentityValueException, TooManyAttempts
	{
		if (credState == null)
			throw new IllegalIdentityValueException("Identity was not resolved.");
		if (answerAttempts >= MAX_ANSWER_ATTEMPTS)
			throw new TooManyAttempts();
		answerAttempts++;
		byte[] answerHash = credState.getAnswerHash();
		String question = credState.getSecurityQuestion();
		if (answerHash == null || question == null)
			throw new IllegalIdentityValueException("Identity has no question set.");

		byte[] testedHash = CryptoUtils.hash(answer.toLowerCase(), question);
		if (!Arrays.areEqual(testedHash, answerHash))
			throw new WrongArgumentException("The answer is incorrect");
	}

	private void createCode()
	{
		int codeLen = settings.getCodeLength();
		char[] codeA = new char[codeLen];
		for (int i=0; i<codeLen; i++)
			codeA[i] = CHARS_POOL[rnd.nextInt(CHARS_POOL.length)];
		codeSent = new String(codeA);
		codeValidityEnd = System.currentTimeMillis() + MAX_CODE_VALIDITY;
	}
	
	@Override
	public void sendCode() throws EngineException
	{
		if (credState == null)
			throw new IllegalIdentityValueException("Identity was not resolved.");
		if (codeSendingAttempts >= MAX_RESENDS)
			throw new TooManyAttempts();
		codeSendingAttempts++;
		if (codeSent == null)
			createCode();

		Map<String, String> params = new HashMap<>();
		params.put(CODE_VAR, codeSent);
		params.put(USER_VAR, subject.getIdentity().getValue());
		notificationProducer.sendNotification(subject, UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL, 
					PASSWORD_RESET_TPL, params);
	}


	@Override
	public void verifyDynamicData(String answer) throws WrongArgumentException, TooManyAttempts
	{
		if (dynamicAnswerAttempts >= MAX_ANSWER_ATTEMPTS)
			throw new TooManyAttempts();
		dynamicAnswerAttempts++;
		if (System.currentTimeMillis() > codeValidityEnd)
			throw new TooManyAttempts();
		if (codeSent == null || !codeSent.equals(answer))
			throw new WrongArgumentException("The code is invalid");
	}
	
	@Override
	public String getCredentialConfiguration()
	{
		return completeCredentialConfiguration;
	}
	
	@Override
	public void updateCredential(String newCredential) throws EngineException
	{
		if (credState == null)
			throw new IllegalStateException("Identity was not resolved.");

		credentialHelper.setCredential(resolved.getEntityId(), credentialId, newCredential, 
				localCredentialHandler);
	}
}
