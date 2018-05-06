/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.engine.api.authn.CredentialReset;
import pl.edu.icm.unity.engine.api.authn.local.CredentialHelper;
import pl.edu.icm.unity.engine.api.authn.local.LocalCredentialVerificator;
import pl.edu.icm.unity.engine.api.identity.IdentityResolver;
import pl.edu.icm.unity.engine.api.notification.NotificationProducer;
import pl.edu.icm.unity.exceptions.IllegalIdentityValueException;
import pl.edu.icm.unity.exceptions.TooManyAttempts;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.stdext.credential.CredentialResetBase;
import pl.edu.icm.unity.types.basic.IdentityTaV;

/**
 * Password reset implementation of {@link CredentialReset}. This implementation is stateful, i.e. from creation it
 * must be used exclusively by a single reset procedure.
 * @author K. Benedyczak
 */
public class PasswordCredentialResetImpl extends CredentialResetBase
{	
	private PasswordCredentialResetSettings settings;
	private PasswordEngine passwordEngine = new PasswordEngine();
	private int answerAttempts = 0;

	public PasswordCredentialResetImpl(NotificationProducer notificationProducer,
			IdentityResolver identityResolver,
			LocalCredentialVerificator localVerificator,
			CredentialHelper credentialHelper, String credentialId,
			ObjectNode completeCredentialConfiguration,
			PasswordCredentialResetSettings settings)
	{
		super(notificationProducer, identityResolver, localVerificator, credentialHelper,
				credentialId, completeCredentialConfiguration);
		this.settings = settings;
	}

	@Override
	protected String getCredentialSettings()
	{
		ObjectNode node = Constants.MAPPER.createObjectNode();
		settings.serializeTo(node);
		return JsonUtil.toJsonString(node);
	}

	@Override
	protected int getCodeLength()
	{
		return settings.getCodeLength();
	}

	@Override
	public String getSecurityQuestion()
	{
		if (!checkSubject())
			return getFakeQuestion();
		PasswordCredentialDBState credState = PasswordCredentialDBState
				.fromJson(resolved.getCredentialValue());
		String q = credState.getSecurityQuestion();
		if (q == null)
			return getFakeQuestion();
		return q;
	}

	private String getFakeQuestion()
	{
		List<String> questions = settings.getQuestions();
		int hash = requestedSubject.getValue().hashCode();
		int num = (hash < 0 ? -hash : hash) % questions.size();
		return questions.get(num);
	}

	@Override
	public void verifyStaticData(String answer) throws WrongArgumentException,
			IllegalIdentityValueException, TooManyAttempts
	{
		if (!checkSubject())
			throw new IllegalIdentityValueException("Identity was not resolved.");
		if (answerAttempts >= MAX_ANSWER_ATTEMPTS)
			throw new TooManyAttempts();
		answerAttempts++;

		PasswordCredentialDBState credState = PasswordCredentialDBState
				.fromJson(resolved.getCredentialValue());
		PasswordInfo storedAnswer = credState.getAnswer();
		String question = credState.getSecurityQuestion();
		if (storedAnswer == null || question == null)
			throw new IllegalIdentityValueException("Identity has no question set.");

		if (!passwordEngine.verify(storedAnswer, answer))
			throw new WrongArgumentException("The answer is incorrect");
	}

	@Override
	public void setSubject(IdentityTaV subject)
	{
		super.setSubject(subject, PasswordVerificator.IDENTITY_TYPES);
		
	}
}
