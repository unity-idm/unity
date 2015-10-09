/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential;

import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.stdext.utils.CryptoUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
	private int answerRehashNumber;

	
	public PasswordCredentialDBState(Deque<PasswordInfo> passwords, boolean outdated,
			String securityQuestion, byte[] answerHash, int answerRehashNumber)
	{
		this.passwords = passwords;
		this.outdated = outdated;
		this.securityQuestion = securityQuestion;
		this.answerRehashNumber = answerRehashNumber;
		this.answerHash = answerHash != null ? Arrays.copyOf(answerHash, answerHash.length) : null;
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
	public int getAnswerRehashNumber()
	{
		return answerRehashNumber;
	}

	public static PasswordCredentialDBState fromJson(String raw) throws InternalException
	{
		if (raw == null || raw.length() == 0)
			return new PasswordCredentialDBState(new LinkedList<PasswordInfo>(), false, null, null, -1);
		JsonNode root;
		try
		{
			root = Constants.MAPPER.readTree(raw);

			JsonNode passwords = root.get("passwords");
			Deque<PasswordInfo> ret = new LinkedList<PasswordInfo>();
			for (int i=0; i<passwords.size(); i++)
			{
				JsonNode rawPasswd = passwords.get(i);
				int rehashNum = rawPasswd.has("rehashNumber") ? 
						rawPasswd.get("rehashNumber").intValue() : 1;
				ret.add(new PasswordInfo(rawPasswd.get("hash").binaryValue(),
						rawPasswd.get("salt").asText(),
						rehashNum,
						rawPasswd.get("time").asLong()));
			}
			boolean outdated = root.get("outdated").asBoolean();
			JsonNode qn = root.get("question");
			String question = qn == null ? null : qn.asText();
			JsonNode an = root.get("answerHash");
			byte[] answerHash = an == null ? null : an.binaryValue();
			int answerRehashNumber = 1;
			if (root.has("answerRehashNumber"))
				answerRehashNumber = root.get("answerRehashNumber").asInt();
			return new PasswordCredentialDBState(ret, outdated, question, answerHash, answerRehashNumber);
		} catch (Exception e)
		{
			throw new InternalException("Can't deserialize password credential from JSON", e);
		}
	}
	
	public static String toJson(PasswordCredential credential, Deque<PasswordInfo> currentPasswords,
			PasswordToken pToken)
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();
		ArrayNode passwords = root.putArray("passwords");
		for (PasswordInfo pi: currentPasswords)
		{
			ObjectNode entry = passwords.addObject();
			entry.put("hash", pi.getHash());
			entry.put("salt", pi.getSalt());
			entry.put("time", pi.getTime().getTime());
			entry.put("rehashNumber", pi.getRehashNumber());
		}
		root.put("outdated", false);
		if (credential.getPasswordResetSettings().isEnabled() && 
				credential.getPasswordResetSettings().isRequireSecurityQuestion())
		{
			String question = credential.getPasswordResetSettings().getQuestions().get(
					pToken.getQuestion());
			root.put("question", question);
			root.put("answerHash", CryptoUtils.hash(pToken.getAnswer().toLowerCase(), question, 
					credential.getRehashNumber()));
			root.put("answerRehashNumber", credential.getRehashNumber());
		}
		try
		{
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize password credential to JSON", e);
		}

	}
}