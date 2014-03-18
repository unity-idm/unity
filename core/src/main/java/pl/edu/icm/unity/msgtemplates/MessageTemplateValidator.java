/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.msgtemplates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.msgtemplates.MessageTemplate.Message;

/**
 * Helper: checks if given message or text has only variables supported by a template consumer. 
 * 
 * @author P. Piernik
 */
public class MessageTemplateValidator
{
	/**
	 * Validates a set of {@link Message}s
	 * @param consumer
	 * @param message
	 * @return
	 * @throws WrongArgumentException 
	 */
	public static void validateMessages(MessageTemplateDefinition consumer, Map<String, Message> messages) 
			throws IllegalVariablesException
	{
		for (Message message: messages.values())
			validateMessage(consumer, message);
	}
	
	/**
	 * Validates a single {@link Message}
	 * @param consumer
	 * @param message
	 * @return
	 * @throws WrongArgumentException 
	 */
	public static void validateMessage(MessageTemplateDefinition consumer, Message message) 
			throws IllegalVariablesException
	{
		validateText(consumer, message.getSubject());
		validateText(consumer, message.getBody());
	}

	public static void validateText(MessageTemplateDefinition consumer, String text) throws IllegalVariablesException
	{
		ArrayList<String> usedField = new ArrayList<String>();
		Pattern pattern = Pattern.compile("\\$\\{[a-zA-Z0-9]*\\}");

		String b = (String) text;
		Matcher matcher = pattern.matcher(b);
		while (matcher.find())
		{
			usedField.add(b.substring(matcher.start() + 2, matcher.end() - 1));

		}
		Set<String> knownVariables = consumer.getVariables().keySet();
		Set<String> unknown = new HashSet<String>();
		for (String f : usedField)
		{
			if (!knownVariables.contains(f))
				unknown.add(f);
		}
		if (!unknown.isEmpty())
			throw new IllegalVariablesException(unknown);
	}
	
	public static class IllegalVariablesException extends Exception
	{
		private Collection<String> unknown;

		public IllegalVariablesException(Collection<String> unknown)
		{
			this.unknown = unknown;
		}

		public Collection<String> getUnknown()
		{
			return unknown;
		}
	}
}
