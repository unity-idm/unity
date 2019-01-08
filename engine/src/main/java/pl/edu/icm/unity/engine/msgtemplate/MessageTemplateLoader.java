/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import eu.unicore.util.configuration.ConfigIncludesProcessor;
import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.configuration.FilePropertiesHelper;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.exceptions.WrongArgumentException;
import pl.edu.icm.unity.types.I18nMessage;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.MessageTemplate;
import pl.edu.icm.unity.types.basic.MessageType;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Loads message templates from file configuration
 * 
 * @author K. Benedyczak
 */
class MessageTemplateLoader
{
	private static final Logger logLegacy = Log.getLegacyLogger(Log.U_SERVER_CFG, 
			MessageTemplateLoader.class);
	private static final org.apache.logging.log4j.Logger log = Log.getLogger(Log.U_SERVER_CFG, MessageTemplateLoader.class);
	
	private MessageTemplateManagement msgTemplatesManagement;
	private NotificationsManagement notificationMan;
	private boolean updateExisting;

	public MessageTemplateLoader(MessageTemplateManagement msgTemplatesManagement,
			NotificationsManagement notificationMan, boolean updateExisting)
	{
		this.msgTemplatesManagement = msgTemplatesManagement;
		this.notificationMan = notificationMan;
		this.updateExisting = updateExisting;
	}

	void initializeMsgTemplates(File file, Predicate<String> filter)
	{
		Properties props = null;
		try
		{
			props = FilePropertiesHelper.load(file);
			boolean newFormat = props.keySet().stream()
					.filter(k -> k.toString().contains(".bodyFile"))
					.findAny().isPresent();
			if (newFormat)
				props = ConfigIncludesProcessor.preprocess(props, logLegacy);
		} catch (IOException e)
		{
			throw new InternalException("Can't load message templates config file", e);
		}
		initializeMsgTemplates(props, filter);
	}
	
	void initializeMsgTemplates(Properties props, Predicate<String> filter)
	{
		Map<String, NotificationChannel> notificationChannels;
		try
		{
			notificationChannels = notificationMan.getNotificationChannels();
		} catch (EngineException e)
		{
			throw new InternalException("Can't load existing notification channels list", e);
		}
		
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
		
		for(String key: templateKeys)
		{
			if (!filter.test(key))
				continue;
			try
			{
				if (updateExisting)
					addOrUpdateMessageTemplate(key, notificationChannels, existingTemplates, props);
				else
					addMessageTemplate(key, notificationChannels, existingTemplates, props);
			} catch (WrongArgumentException e)
			{
				log.error("Template with id " + key + " is invalid, reason: " + e.getMessage(), e);
			} catch (EngineException e)
			{
				log.error("Cannot add template " + key, e);
			}
		}
	}
	
	private void addMessageTemplate(String key, Map<String, NotificationChannel> notificationChannels,
			Map<String, MessageTemplate> existingTemplates, Properties props) throws EngineException
	{
		if (existingTemplates.containsKey(key))
			return;
		log.info("Installing message template {}", key);
		MessageTemplate templ = loadTemplate(props, key);
		if (!verifyNotificationChannelExists(templ, notificationChannels))
			return;
		msgTemplatesManagement.addTemplate(templ);
	}

	private void addOrUpdateMessageTemplate(String key, Map<String, NotificationChannel> notificationChannels,
			Map<String, MessageTemplate> existingTemplates, Properties props) throws EngineException
	{
		MessageTemplate templ = loadTemplate(props, key);
		if (!verifyNotificationChannelExists(templ, notificationChannels))
			return;
		if (existingTemplates.containsKey(key))
		{
			log.info("Updating message template {}", key);
			msgTemplatesManagement.updateTemplate(templ);
		} else
		{
			log.info("Installing message template {}", key);
			msgTemplatesManagement.addTemplate(templ);
		}
	}
	
	private boolean verifyNotificationChannelExists(MessageTemplate templ, 
			Map<String, NotificationChannel> notificationChannels)
	{
		String channel = templ.getNotificationChannel();
		if (!channel.isEmpty() && !notificationChannels.keySet().contains(channel))
		{
			log.debug("Skip adding message template {}: configured notification channel {} does not exist",
					templ.getName(), channel);
			return false;
		}
		return true;
	}
	
	private MessageTemplate loadTemplate(Properties properties, String id) throws WrongArgumentException
	{
		String consumer = properties.getProperty(id + ".consumer", "");
		String description = properties.getProperty(id + ".description", "");
		String typeStr = properties.getProperty(id + ".type", MessageType.PLAIN.name());
		String notificationChannel = properties.getProperty(id + ".notificationChannel",
				"");
		
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
		return new MessageTemplate(id, description, tempMsg, consumer, type, notificationChannel);
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
