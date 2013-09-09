/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.server.authn;

import java.util.ArrayList;
import java.util.List;

import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Credential reset settings.
 * @author K. Benedyczak
 */
public class CredentialResetSettings
{
	private boolean enabled = false;
	private boolean requireEmailConfirmation = true;
	private boolean requireSecurityQuestion = true;
	private int codeLength = 4;
	private List<String> questions = new ArrayList<>();
	
	public CredentialResetSettings()
	{
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enable)
	{
		this.enabled = enable;
	}

	public void setRequireEmailConfirmation(boolean requireEmailConfirmation)
	{
		this.requireEmailConfirmation = requireEmailConfirmation;
	}

	public void setRequireSecurityQuestion(boolean requireSecurityQuestion)
	{
		this.requireSecurityQuestion = requireSecurityQuestion;
	}

	public boolean isRequireEmailConfirmation()
	{
		return requireEmailConfirmation;
	}

	public boolean isRequireSecurityQuestion()
	{
		return requireSecurityQuestion;
	}

	public int getCodeLength()
	{
		return codeLength;
	}

	public void setCodeLength(int codeLength)
	{
		this.codeLength = codeLength;
	}
	
	public List<String> getQuestions()
	{
		return questions;
	}

	public void setQuestions(List<String> questions)
	{
		this.questions = questions;
	}

	public void serializeTo(ObjectNode node)
	{
		node.put("enable", enabled);
		if (!enabled)
			return;
		node.put("codeLength", codeLength);
		node.put("requireEmailConfirmation", requireEmailConfirmation);
		node.put("requireSecurityQuestion", requireSecurityQuestion);
		ArrayNode questionsNode = node.putArray("questions");
		for (String question: questions)
			questionsNode.add(question);
	}
	
	public void deserializeFrom(ObjectNode node)
	{
		this.enabled = node.get("enable").asBoolean();
		if (!enabled)
			return;
		this.codeLength = node.get("codeLength").asInt();
		this.requireEmailConfirmation = node.get("requireEmailConfirmation").asBoolean();
		this.requireSecurityQuestion = node.get("requireSecurityQuestion").asBoolean();
		ArrayNode questionsNode = (ArrayNode) node.get("questions");
		if (questionsNode.size() == 0 && requireSecurityQuestion)
			throw new InternalException("At least one security question must be defined " +
					"if questions are required");
		for (int i=0; i<questionsNode.size(); i++)
			this.questions.add(questionsNode.get(i).asText());
	}
}
