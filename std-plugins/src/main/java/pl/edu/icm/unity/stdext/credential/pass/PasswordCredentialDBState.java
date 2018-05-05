/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.Deque;
import java.util.LinkedList;

import com.fasterxml.jackson.core.JsonProcessingException;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * In DB representation of the credential state.
 * @author K. Benedyczak
 */
class PasswordCredentialDBState
{
	private Deque<PasswordInfo> passwords;
	private boolean outdated;
	private String outdatedReason;
	private String securityQuestion;
	private PasswordInfo answer;

	
	public PasswordCredentialDBState(Deque<PasswordInfo> passwords, boolean outdated,
			String outdatedReason,
			String securityQuestion, PasswordInfo answer)
	{
		this.passwords = passwords;
		this.outdated = outdated;
		this.outdatedReason = outdatedReason;
		this.securityQuestion = securityQuestion;
		this.answer = answer;
	}
	
	protected PasswordCredentialDBState()
	{
	}
	
	public Deque<PasswordInfo> getPasswords()
	{
		return passwords;
	}
	public boolean isOutdated()
	{
		return outdated;
	}
	public String getSecurityQuestion()
	{
		return securityQuestion;
	}
	public String getOutdatedReason()
	{
		return outdatedReason;
	}
	public PasswordInfo getAnswer()
	{
		return answer;
	}

	public static PasswordCredentialDBState fromJson(String raw) throws InternalException
	{
		if (raw == null || raw.length() == 0)
			return new PasswordCredentialDBState(new LinkedList<>(), false, 
					null, null, null);
		try
		{
			return Constants.MAPPER.readValue(raw, PasswordCredentialDBState.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}
	}
	
	public static String toJson(PasswordCredential credential, Deque<PasswordInfo> currentPasswords,
			int questionIndex, PasswordInfo questionAnswer)
	{
		String securityQuestion = null;
		if (credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion())
		{
			securityQuestion = credential.getPasswordResetSettings().getQuestions().get(
					questionIndex);
		}
		
		PasswordCredentialDBState dbState = new PasswordCredentialDBState(
				currentPasswords, false, null, securityQuestion, questionAnswer);
		try
		{
			return Constants.MAPPER.writeValueAsString(dbState);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize password credential to JSON", e);
		}
	}
}