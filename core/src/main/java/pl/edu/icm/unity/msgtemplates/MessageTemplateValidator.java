/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.msgtemplates;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;

/**
 * 
 * Validator helper
 * 
 * @author P. Piernik
 * 
 */

public class MessageTemplateValidator
{
	protected MessageTemplateConsumer consumer;

	public MessageTemplateValidator(MessageTemplateConsumer consumer)
	{
		this.consumer = consumer;
	}

	public void setConsumer(MessageTemplateConsumer consumer)
	{
		this.consumer = consumer;

	}

	public boolean validateMessages(MessageTemplateConsumer consumer,
			Map<String, Message> messages)
	{
		this.consumer = consumer;
		return validateMessages(messages);
	}

	public boolean validateMessages(Map<String, Message> messages)
	{
		for (Message m : messages.values())
		{
			if (!(validateText(m.getSubject()) && validateText(m.getBody())))
				return false;
		}
		return true;
	}

	public boolean validateText(String text)
	{
		ArrayList<String> usedField = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9]*\\}");

		if (consumer == null)
			return false;

		String b = (String) text;
		Matcher matcher = pattern.matcher(b);
		while (matcher.find())
		{
			usedField.add(b.substring(matcher.start() + 2, matcher.end() - 1));

		}
		boolean val = true;
		for (String f : usedField)
		{
			if (!consumer.getVariables().keySet().contains(f))
			{
				val = false;
				break;
			}
		}
		return val;
	}

}
