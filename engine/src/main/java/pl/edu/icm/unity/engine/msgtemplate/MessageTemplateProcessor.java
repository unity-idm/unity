/*
 * Copyright (c) 2018 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import java.util.Map;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageTemplate.Message;

public class MessageTemplateProcessor
{
	public Message getMessage(MessageTemplate template, String locale, String defaultLocale, Map<String, String> params,
			Map<String, MessageTemplate> genericTemplates)
	{
		MessageTemplate preprocessed = preprocessMessage(template, genericTemplates);
		String subject = preprocessed.getMessage().getSubject().getValue(locale, defaultLocale);
		String body = preprocessed.getMessage().getBody().getValue(locale, defaultLocale);
		Message ret = new Message(subject, body, template.getType());
		for (Map.Entry<String, String> paramE: params.entrySet())
		{
			if (paramE.getValue() == null)
				continue;
			ret.setSubject(ret.getSubject().replace("${" + paramE.getKey() + "}", paramE.getValue()));
			ret.setBody(ret.getBody().replace("${" + paramE.getKey() + "}", paramE.getValue()));
			//alt syntax
			ret.setSubject(ret.getSubject().replace("{{" + paramE.getKey() + "}}", paramE.getValue()));
			ret.setBody(ret.getBody().replace("{{" + paramE.getKey() + "}}", paramE.getValue()));
		}
		return ret;
	}

	public MessageTemplate preprocessMessage(MessageTemplate template, Map<String, MessageTemplate> genericTemplates)
	{
		I18nString srcBody = template.getMessage().getBody();
		String def = preprocessString(srcBody.getDefaultValue(), genericTemplates, null);
		I18nString preprocessedBody = new I18nString(def);
		for (Map.Entry<String, String> entry: srcBody.getMap().entrySet())
			preprocessedBody.addValue(entry.getKey(), 
					preprocessString(entry.getValue(), genericTemplates, 
							entry.getKey()));
		I18nMessage processedMessage = new I18nMessage(template.getMessage().getSubject(), 
				preprocessedBody);
		return new MessageTemplate(template.getName(), template.getDescription(), processedMessage, 
				template.getConsumer(), template.getType(), template.getNotificationChannel());
	}

	private String preprocessString(String source, Map<String, MessageTemplate> genericTemplates,
			String locale)
	{
		if (source == null)
			return null;
		String work = source;
		for (Map.Entry<String, MessageTemplate> genericTemplate: genericTemplates.entrySet())
		{
			I18nString included = genericTemplate.getValue().getMessage().getBody();
			String includedString = locale == null ? included.getDefaultValue() : 
				included.getValueRaw(locale);
			if (includedString == null)
				includedString = included.getDefaultValue();
			if (includedString != null)
			{
				String key = MessageTemplateDefinition.INCLUDE_PREFIX + genericTemplate.getKey();
				work = work.replace("${" + key + "}", includedString);
				work = work.replace("{{" + key + "}}", includedString);
			}
		}
		return work;
	}
}
