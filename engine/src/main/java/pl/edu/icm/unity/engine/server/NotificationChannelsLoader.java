/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.EXTERNAL_NOTIFICATION_FILE;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.EXTERNAL_NOTIFICATION_NAME;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.EXTERNAL_NOTIFICATION_PFX;
import static pl.edu.icm.unity.engine.api.config.UnityServerConfiguration.EXTERNAL_NOTIFICATION_SUPPORTS_TEMPLATES;

import java.io.File;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.exceptions.EngineException;
import pl.edu.icm.unity.base.json.JsonUtil;
import pl.edu.icm.unity.base.notifications.NotificationChannel;
import pl.edu.icm.unity.base.notifications.NotificationChannelInfo;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.notifications.email.EmailFacility;
import pl.edu.icm.unity.engine.notifications.script.GroovyEmailNotificationFacility;
import pl.edu.icm.unity.engine.notifications.script.GroovyNotificationChannelConfig;
import pl.edu.icm.unity.engine.notifications.sms.SMSFacility;

/**
 * Loads notification channels. Currently we allow only for a single instance of each type,
 * however in principle the engine is capable of many instances with different settings.
 * 
 * @author K. Benedyczak
 */
@Component
class NotificationChannelsLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG,
			NotificationChannelsLoader.class);
	
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notManagement;
	@Autowired
	private UnityServerConfiguration config;
	
	void initialize()
	{
		clean();
		initializeEmailChannel();
		initializeSMSChannel();
		initializeExternalChannels();
	}

	private void initializeExternalChannels()
	{
		Set<String> channelKeys = config.getStructuredListKeys(EXTERNAL_NOTIFICATION_PFX);
		for (String channelKey: channelKeys)
		{
			String file = config.getValue(channelKey + EXTERNAL_NOTIFICATION_FILE);
			String name = config.getValue(channelKey + EXTERNAL_NOTIFICATION_NAME);
			boolean supportsTemplates = config.getBooleanValue(channelKey + EXTERNAL_NOTIFICATION_SUPPORTS_TEMPLATES);
			String channelId = channelKey.substring(EXTERNAL_NOTIFICATION_PFX.length());
			channelId = channelId.substring(0, channelId.length()-1);
			GroovyNotificationChannelConfig config = new GroovyNotificationChannelConfig(file, supportsTemplates);
			NotificationChannel emailCh = new NotificationChannel(
					channelId, 
					name, 
					JsonUtil.toJsonString(config), 
					GroovyEmailNotificationFacility.NAME);
			try
			{
				notManagement.addNotificationChannel(emailCh);
			} catch (EngineException e)
			{
				throw new ConfigurationException("Can't load external e-mail notification channel", e);
			}
			log.info("Created a notification channel: " + emailCh.getName() + " [" + 
					emailCh.getFacilityId() + "]");
		}
	}

	private void clean()
	{
		try
		{
			Map<String, NotificationChannelInfo> existingChannels = notManagement.getNotificationChannels();
			for (String key: existingChannels.keySet())
			{
				notManagement.removeNotificationChannel(key);
				log.info("Removed old definition of the notification channel " + key);
			}
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't cleanup old notification channels", e);
		}
	}
	
	private void initializeEmailChannel()
	{
		try
		{
			if (!config.isSet(UnityServerConfiguration.MAIL_CONF))
			{
				log.info("Mail configuration file is not set, mail notification channel won't be loaded.");
				return;
			}
			File mailCfgFile = config.getFileValue(UnityServerConfiguration.MAIL_CONF, false);
			String mailCfg = FileUtils.readFileToString(mailCfgFile, Charset.defaultCharset());
			NotificationChannel emailCh = new NotificationChannel(
					UnityServerConfiguration.DEFAULT_EMAIL_CHANNEL, 
					"Default email channel", mailCfg, EmailFacility.NAME);
			notManagement.addNotificationChannel(emailCh);
			log.info("Created a notification channel: " + emailCh.getName() + " [" + 
					emailCh.getFacilityId() + "]");
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't load e-mail notification channel configuration", e);
		}
	}
	
	private void initializeSMSChannel()
	{
		try
		{
			if (!config.isSet(UnityServerConfiguration.SMS_CONF))
			{
				log.info("SMS configuration file is not set, SMS notification channel won't be loaded.");
				return;
			}
			File smsCfgFile = config.getFileValue(UnityServerConfiguration.SMS_CONF, false);
			String smsCfg = FileUtils.readFileToString(smsCfgFile, Charset.defaultCharset());
			NotificationChannel smsCh = new NotificationChannel(
					UnityServerConfiguration.DEFAULT_SMS_CHANNEL, 
					"Default SMS channel", smsCfg, SMSFacility.NAME);
			notManagement.addNotificationChannel(smsCh);
			log.info("Created a notification channel: " + smsCh.getName() + " [" + 
					smsCh.getFacilityId() + "]");
		} catch (Exception e)
		{
			throw new ConfigurationException("Can't load SMS notification channel configuration", e);
		}		
	}
}
