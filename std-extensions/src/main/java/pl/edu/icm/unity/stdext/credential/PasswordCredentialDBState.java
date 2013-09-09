/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.Deque;
import java.util.LinkedList;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * In DB representation of the credential state.
 * @author K. Benedyczak
 */
class PasswordCredentialDBState
{
	private Deque<PasswordInfo> passwords;
	private boolean outdated;
	private String securityQuestion;
	private byte[] answerHash;

	
	public PasswordCredentialDBState(Deque<PasswordInfo> passwords, boolean outdated,
			String securityQuestion, byte[] answerHash)
	{
		this.passwords = passwords;
		this.outdated = outdated;
		this.securityQuestion = securityQuestion;
		this.answerHash = answerHash;
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
	public byte[] getAnswerHash()
	{
		return answerHash;
	}
	
	public static PasswordCredentialDBState fromJson(String raw) throws InternalException
	{
		if (raw == null || raw.length() == 0)
			return new PasswordCredentialDBState(new LinkedList<PasswordInfo>(), false, null, null);
		JsonNode root;
		try
		{
			root = Constants.MAPPER.readTree(raw);

			JsonNode passwords = root.get("passwords");
			Deque<PasswordInfo> ret = new LinkedList<PasswordInfo>();
			for (int i=0; i<passwords.size(); i++)
			{
				JsonNode rawPasswd = passwords.get(i);
				ret.add(new PasswordInfo(rawPasswd.get("hash").binaryValue(),
						rawPasswd.get("salt").asText(),
						rawPasswd.get("time").asLong()));
			}
			boolean outdated = root.get("outdated").asBoolean();
			JsonNode qn = root.get("question");
			String question = qn == null ? null : qn.asText();
			JsonNode an = root.get("answerHash");
			byte[] answerHash = an == null ? null : an.binaryValue();
			return new PasswordCredentialDBState(ret, outdated, question, answerHash);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}
	}
}