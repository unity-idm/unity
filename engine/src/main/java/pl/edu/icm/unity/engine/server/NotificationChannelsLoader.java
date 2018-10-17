/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.server;

import java.io.File;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.base.utils.Log;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;
import pl.edu.icm.unity.engine.notifications.EmailFacility;
import pl.edu.icm.unity.engine.notifications.sms.SMSFacility;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Loads notification channels. Currently we allow only for a single instance of each type,
 * however in principle the engine is capable of many instances with different settings.
 * 
 * @author K. Benedyczak
 */
@Component
public class NotificationChannelsLoader
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_CFG,
			NotificationChannelsLoader.class);
	
	@Autowired
	@Qualifier("insecure")
	private NotificationsManagement notManagement;
	@Autowired
	private UnityServerConfiguration config;
	
	public void initialize()
	{
		clean();
		initializeEmailChannel();
		initializeSMSChannel();
	}

	private void clean()
	{
		try
		{
			Map<String, NotificationChannel> existingChannels = notManagement.getNotificationChannels();
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
			String mailCfg = FileUtils.readFileToString(mailCfgFile);
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
			String smsCfg = FileUtils.readFileToString(smsCfgFile);
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
