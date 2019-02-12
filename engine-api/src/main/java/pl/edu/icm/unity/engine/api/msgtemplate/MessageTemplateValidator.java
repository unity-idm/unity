/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.api.msgtemplate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;

/**
 * Helper: checks if given message or text has only variables supported by a template consumer. 
 * 
 * @author P. Piernik
 */
public class MessageTemplateValidator
{
	/**
	 * Validates a single {@link Message}
	 * @param consumer
	 * @param message
	 * @return
	 * @throws MandatoryVariablesException 
	 * @throws WrongArgumentException 
	 */
	public static void validateMessage(MessageTemplateDefinition consumer, I18nMessage message) 
			throws IllegalVariablesException, MandatoryVariablesException
	{
		I18nString subject = message.getSubject();
		for (String subjectL: subject.getMap().values())
			validateText(consumer, subjectL, false);
		I18nString body = message.getBody();
		for (String bodyL: body.getMap().values())
			validateText(consumer, bodyL, true);
	}

	/**
	 * @return all variables used in message template (union over all language variants)
	 */
	public static Set<String> extractVariables(I18nMessage message)
	{
		Set<String> vars = new HashSet<>();
		I18nString subject = message.getSubject();
		for (String subjectL: subject.getMap().values())
			vars.addAll(extractVariables(subjectL));
		vars.addAll(extractVariables(subject.getDefaultValue()));
		
		I18nString body = message.getBody();
		for (String bodyL: body.getMap().values())
			vars.addAll(extractVariables(bodyL));
		vars.addAll(extractVariables(body.getDefaultValue()));
		return vars;
	}
	/**
	 * @return all non-built-in variables used in message template (union over all language variants)
	 */
	public static Set<String> extractCustomVariables(I18nMessage message)
	{
		return extractVariables(message).stream()
				.filter(var -> var.startsWith(MessageTemplateDefinition.CUSTOM_VAR_PREFIX))
				.collect(Collectors.toSet());
	}
	
	private static Set<String> extractVariables(String text)
	{
		if (text == null)
			return Collections.emptySet();
		Set<String> usedField = extractVariables(text, "\\$\\{[^\\}]*\\}", 1);
		usedField.addAll(extractVariables(text, "\\{\\{[^\\}]*\\}\\}", 2));
		return filterDirectives(usedField);
	}

	private static Set<String> filterDirectives(Set<String> statements)
	{
		return statements.stream()
				.filter(v -> !v.startsWith(MessageTemplateDefinition.INCLUDE_PREFIX))
				.collect(Collectors.toSet());
	}
	
	private static Set<String> extractVariables(String text, String patternStr, int suffixLen)
	{
		Set<String> usedField = new HashSet<>();
		Pattern pattern = Pattern.compile(patternStr);
		String b = (String) text;
		Matcher matcher = pattern.matcher(b);
		while (matcher.find())
		{
			usedField.add(b.substring(matcher.start() + 2, matcher.end() - suffixLen));

		}
		return usedField;
	}

	public static void validateText(MessageTemplateDefinition consumer, String text, boolean checkMandatory) 
			throws IllegalVariablesException, MandatoryVariablesException
	{
		Set<String> usedField = extractVariables(text);

		Set<String> knownVariables = new HashSet<>();
		Set<String> mandatory = new HashSet<>();
		for (MessageTemplateVariable var : consumer.getVariables().values())
		{
			knownVariables.add(var.getName());
			if (var.isMandatory())
				mandatory.add(var.getName());
		}
		
		Set<String> unknown = new HashSet<>();
		for (String f : usedField)
		{
			if (!knownVariables.contains(f))
				unknown.add(f);
		}
		if (!unknown.isEmpty())
		{
			if (consumer.allowCustomVariables())
			{
				boolean hasNonCustomUnnown = unknown.stream()
					.filter(var -> !var.startsWith(MessageTemplateDefinition.CUSTOM_VAR_PREFIX))
					.findAny().isPresent();
				if (hasNonCustomUnnown)
					throw new IllegalVariablesException(unknown);
			} else
			{
				throw new IllegalVariablesException(unknown);
			}
		}
		
		if (!checkMandatory)
			return;
		Set<String> uman = new HashSet<>();
		for (String m : mandatory)
		{
			if (!usedField.contains(m))
				uman.add(m);
		}
		if (!uman.isEmpty())
		{
			throw new MandatoryVariablesException(uman);
		}
			
		
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
	
	public static class MandatoryVariablesException extends Exception
	{
		private Collection<String> mandatory;

		public MandatoryVariablesException(Collection<String> mandatory)
		{
			this.mandatory = mandatory;
		}

		public Collection<String> getMandatory()
		{
			return mandatory;
		}
	}
}
