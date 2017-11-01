/*
 * Copyright (c) 2017 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigIncludesProcessor;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.FilePropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;

/**
 * Loads message templates from file configuration
 * 
 * @author K. Benedyczak
 */
@Component
class MessageTemplateLoader
{
	private static final Logger log = Log.getLegacyLogger(Log.U_SERVER_CFG, 
			MessageTemplateLoader.class);
	
	private MessageTemplateManagement msgTemplatesManagement;
	
	@Autowired
	public MessageTemplateLoader(
			@Qualifier("insecure") MessageTemplateManagement msgTemplatesManagement)
	{
		this.msgTemplatesManagement = msgTemplatesManagement;
	}

	void initializeMsgTemplates(File file)
	{
		Properties props = null;
		try
		{
			props = FilePropertiesHelper.load(file);
			props = ConfigIncludesProcessor.preprocess(props, log);
		} catch (IOException e)
		{
			throw new InternalException("Can't load message templates config file", e);
		}
		initializeMsgTemplates(props);
	}
	
	void initializeMsgTemplates(Properties props)
	{
		Map<String, MessageTemplate> existingTemplates;
		try
		{
			existingTemplates = msgTemplatesManagement.listTemplates();
		} catch (EngineException e)
		{
			throw new InternalException("Can't load existing message templates list", e);
		}
		
		
		Set<String> templateKeys = new HashSet<>();
		for (Object keyO: props.keySet())
		{
			String key = (String) keyO;
			if (key.contains("."))
				templateKeys.add(key.substring(0, key.indexOf('.')));
		}	
		
		for(String key:templateKeys)
		{
			if (existingTemplates.containsKey(key))
			{
				continue;
			}
			try
			{
				MessageTemplate templ = loadTemplate(props, key);
				msgTemplatesManagement.addTemplate(templ);
			} catch (WrongArgumentException e)
			{
				log.error("Template with id " + key + " is invalid, reason: "
						+ e.getMessage(), e);
			} catch (EngineException e)
			{
				log.error("Cannot add template " + key, e);
			}
		}
		
	}
	
	private MessageTemplate loadTemplate(Properties properties, String id) throws WrongArgumentException
	{
		String consumer = properties.getProperty(id+".consumer", "");
		String description = properties.getProperty(id+".description", "");
		String typeStr = properties.getProperty(id+".type", MessageType.PLAIN.name());

		MessageType type;
		try
		{
			type = MessageType.valueOf(typeStr);
		} catch (Exception e)
		{
			throw new ConfigurationException("Invalid template type: " + typeStr + ", "
					+ "for template id " + id + 
					", supported values are: " + MessageType.values(), e);
		}
		
		I18nString subjectI18 = getSubject(properties, id);
		I18nString bodyI18 = getBody(properties, id);
		I18nMessage tempMsg = new I18nMessage(subjectI18, bodyI18);
		return new MessageTemplate(id, description, tempMsg, consumer, type);
	}

	private I18nString getSubject(Properties properties, String id)
	{
		String prefix = id + ".subject";
		I18nString ret = new I18nString();
		loadLocalized(properties, prefix, 
				e -> ret.addValue(e.getKey(), e.getValue()));
		if (ret.isEmpty())
			throw new ConfigurationException("Template with id " + id 
					+ " must have subject defined");
		return ret;
	}

	private I18nString getBody(Properties properties, String id)
	{
		String prefixInline = id + ".body";
		String prefixFile = id + ".bodyFile";
		I18nString ret = new I18nString();
		loadLocalized(properties, prefixInline, 
				e -> ret.addValue(e.getKey(), e.getValue()));

		loadLocalized(properties, prefixFile, 
				e -> ret.addValue(e.getKey(), loadFile(e.getValue(), id)));
		if (ret.isEmpty())
			throw new ConfigurationException("Template with id " + id
					+ " must have body defined");
		return ret;
	}

	private String loadFile(String bodyFile, String id)
	{
		try
		{
			return FileUtils.readFileToString(new File(bodyFile));
		} catch (IOException e)
		{
			throw new ConfigurationException("Problem loading template " + id + " bodyFile "
					+ bodyFile + ", reason: " + e.getMessage(), e);
		}
	}

	private void loadLocalized(Properties properties, String prefix, 
			Consumer<Map.Entry<String, String>> consumer)
	{
		Collection<String> keys = getKeysWtihPrefix(properties, prefix);
		Set<String> locales = getLocales(keys, prefix);
		for (String locale: locales)
		{
			String value = properties.getProperty(prefix + (locale == null ? "" : "." + locale));
			consumer.accept(new AbstractMap.SimpleEntry<>(locale, value));
		}
	}

	
	private Set<String> getLocales(Collection<String> keys, String pfx)
	{
		int len = pfx.length();
		return keys.stream()
				.map(k -> k.substring(len))
				.map(k -> k.startsWith(".") ? k.substring(1) : k)
				.map(k -> k.equals("") ? null : k)
				.collect(Collectors.toSet());
	}

	
	private Collection<String> getKeysWtihPrefix(Properties properties, String id)
	{
		return properties.keySet().stream()
				.map(k -> k.toString())
				.filter(k -> k.startsWith(id + ".") || k.equals(id))
				.collect(Collectors.toSet());
	}
}
