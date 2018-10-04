/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Credential reset settings.
 * @author K. Benedyczak
 */
public class PasswordCredentialResetSettings
{
	
	public enum ConfirmationMode {RequireEmail, RequireMobile, RequireEmailAndMobile, RequireEmailOrMobile, NothingRequire}
	
	private boolean enabled = false;
	private boolean requireSecurityQuestion = true;
	private int codeLength = 4;
	private List<String> questions = new ArrayList<>();
	private String emailSecurityCodeMsgTemplate;
	private String mobileSecurityCodeMsgTemplate;
	private ConfirmationMode confirmationMode = ConfirmationMode.NothingRequire;
	
	public PasswordCredentialResetSettings()
	{
	}
	
	public PasswordCredentialResetSettings(ObjectNode node)
	{
		deserializeFrom(node);
	}

	public boolean isEnabled()
	{
		return enabled;
	}

	public void setEnabled(boolean enable)
	{
		this.enabled = enable;
	}

	public void setRequireSecurityQuestion(boolean requireSecurityQuestion)
	{
		this.requireSecurityQuestion = requireSecurityQuestion;
	}

	public boolean isRequireEmailConfirmation()
	{
		return confirmationMode.equals(ConfirmationMode.RequireEmail)
				|| confirmationMode.equals(ConfirmationMode.RequireEmailAndMobile);

	}
	
	public boolean isRequireMobileConfirmation()
	{
		return confirmationMode.equals(ConfirmationMode.RequireMobile)
				|| confirmationMode.equals(ConfirmationMode.RequireEmailAndMobile);
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

	public String getEmailSecurityCodeMsgTemplate()
	{
		return emailSecurityCodeMsgTemplate;
	}

	public void setEmailSecurityCodeMsgTemplate(String emailSecurityCodeMsgTemplate)
	{
		this.emailSecurityCodeMsgTemplate = emailSecurityCodeMsgTemplate;
	}

	public String getMobileSecurityCodeMsgTemplate()
	{
		return mobileSecurityCodeMsgTemplate;
	}

	public void setMobileSecurityCodeMsgTemplate(String mobileSecurityCodeMsgTemplate)
	{
		this.mobileSecurityCodeMsgTemplate = mobileSecurityCodeMsgTemplate;
	}

	public void serializeTo(ObjectNode node)
	{
		node.put("enable", enabled);
		if (!enabled)
			return;
		node.put("codeLength", codeLength);
		node.put("confirmationMode", confirmationMode.toString());
		node.put("requireSecurityQuestion", requireSecurityQuestion);
		ArrayNode questionsNode = node.putArray("questions");
		for (String question: questions)
			questionsNode.add(question);
		node.put("emailSecurityCodeMsgTemplate", emailSecurityCodeMsgTemplate);
		node.put("mobileSecurityCodeMsgTemplate", mobileSecurityCodeMsgTemplate);
	}
	
	public void deserializeFrom(ObjectNode node)
	{		
		this.enabled = node.get("enable").asBoolean();
		if (!enabled)
			return;
		this.codeLength = node.get("codeLength").asInt();
		this.confirmationMode = ConfirmationMode.valueOf(node.get("confirmationMode").asText());
		this.requireSecurityQuestion = node.get("requireSecurityQuestion").asBoolean();
		ArrayNode questionsNode = (ArrayNode) node.get("questions");
		if (requireSecurityQuestion)
		{
			if (questionsNode == null || questionsNode.size() == 0)
				throw new InternalException("At least one security question must be defined " +
					"if questions are required");
			for (int i=0; i<questionsNode.size(); i++)
				this.questions.add(questionsNode.get(i).asText());
		}
		if (node.has("emailSecurityCodeMsgTemplate") && !node.get("emailSecurityCodeMsgTemplate").isNull())
			emailSecurityCodeMsgTemplate = node.get("emailSecurityCodeMsgTemplate").asText();
		else
			emailSecurityCodeMsgTemplate = "PasswordResetCode"; //backwards compatibility
		
		if (node.has("mobileSecurityCodeMsgTemplate") && !node.get("mobileSecurityCodeMsgTemplate").isNull())
			mobileSecurityCodeMsgTemplate = node.get("mobileSecurityCodeMsgTemplate").asText();
		validate();
	}	
	
	public void validate()
	{

		if ((confirmationMode.equals(ConfirmationMode.RequireEmail)
				|| confirmationMode.equals(ConfirmationMode.RequireEmailAndMobile)
				|| confirmationMode.equals(ConfirmationMode.RequireEmailOrMobile))
				&& (emailSecurityCodeMsgTemplate == null
						|| emailSecurityCodeMsgTemplate.isEmpty()))
			throw new InternalException("Email reset code message template must be defined");
		
		if ((confirmationMode.equals(ConfirmationMode.RequireMobile)
				|| confirmationMode.equals(ConfirmationMode.RequireEmailAndMobile)
				|| confirmationMode.equals(ConfirmationMode.RequireEmailOrMobile))
				&& (mobileSecurityCodeMsgTemplate == null
						|| mobileSecurityCodeMsgTemplate.isEmpty()))
			throw new InternalException("Mobile reset code message template must be defined");
		
		if (confirmationMode.equals(ConfirmationMode.NothingRequire)
				&& !requireSecurityQuestion)
			throw new InternalException(
					"Security question must be defined or another option of confirmation method must be choosen");		
	}

	public ConfirmationMode getConfirmationMode()
	{
		return confirmationMode;
	}

	public void setConfirmationMode(ConfirmationMode confirmationMode)
	{
		this.confirmationMode = confirmationMode;
	}
}
