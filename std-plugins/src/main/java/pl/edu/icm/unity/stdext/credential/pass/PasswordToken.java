/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.stdext.credential.pass;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.Constants;
import pl.edu.icm.unity.exceptions.IllegalCredentialException;
import pl.edu.icm.unity.exceptions.InternalException;

/**
 * Used to express a new password for {@link PasswordVerificator}. Implements serialization to/from JSON.
 * <pre>
 *  {
 *  "existingPassword" : "existingPassword"
 *  "password": "password",
 *  "question": "NUMBER",
 *  "answer": "answer"
 * }
 * </pre>
 * @author K. Benedyczak
 */
public class PasswordToken
{
	private String password;
	private String existingPassword = null;
	private int question = -1;
	private String answer = null;
	
	private PasswordToken()
	{
	}

	public PasswordToken(String password)
	{
		this.password = password;
	}

	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public String getExistingPassword()
	{
		return existingPassword;
	}

	public void setExistingPassword(String existingPassword)
	{
		this.existingPassword = existingPassword;
	}

	public int getQuestion()
	{
		return question;
	}

	public void setQuestion(int question)
	{
		this.question = question;
	}

	public String getAnswer()
	{
		return answer;
	}

	public void setAnswer(String answer)
	{
		this.answer = answer;
	}

	public JsonNode toJsonNode()
	{
		ObjectNode root = Constants.MAPPER.createObjectNode();

		if (existingPassword != null)
			root.put("existingPassword", existingPassword);
		root.put("password", password);
		if (answer != null)
		{
			root.put("answer", answer);
			root.put("question", question);
		}
		return root;
	}
	
	public String toJson()
	{
		try
		{
			JsonNode root = toJsonNode();
			return Constants.MAPPER.writeValueAsString(root);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't serialize password credential to JSON", e);
		}
	}
	
	public static PasswordToken loadFromJson(String json) throws IllegalCredentialException
	{
		try
		{
			PasswordToken ret = new PasswordToken();
			ObjectNode inputNode = (ObjectNode) Constants.MAPPER.readTree(json);
			ret.password = inputNode.get("password").asText();
			if (inputNode.has("existingPassword"))
				ret.existingPassword = inputNode.get("existingPassword").asText();
			if (inputNode.has("answer"))
				ret.answer = inputNode.get("answer").asText();
			if (inputNode.has("question"))
				ret.question = inputNode.get("question").asInt();
			return ret;
		} catch (Exception e)
		{
			throw new IllegalCredentialException("The supplied credential definition has invalid syntax", e);
		}
	}
}
