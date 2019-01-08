/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.msgtemplate;

import java.io.File;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.engine.api.MessageTemplateManagement;
import pl.edu.icm.unity.engine.api.NotificationsManagement;
import pl.edu.icm.unity.engine.api.config.UnityServerConfiguration;

/**
 * Loads message templates from file configuration - insecure, useful forr use at startup.
 * @author K. Benedyczak
 */
@Component
public class MessageTemplateInitializatior
{
	private MessageTemplateLoader loader;
	private File configFile;
	
	@Autowired
	public MessageTemplateInitializatior(
			@Qualifier("insecure") MessageTemplateManagement msgTemplatesManagement,
			@Qualifier("insecure") NotificationsManagement notificationMan,
			UnityServerConfiguration config)
	{
		this.loader = new MessageTemplateLoader(msgTemplatesManagement, notificationMan,
				config.getBooleanValue(UnityServerConfiguration.RELOAD_MSG_TEMPLATES));
		configFile = config.getFileValue(UnityServerConfiguration.TEMPLATES_CONF, false);
	}

	public void initializeMsgTemplates()
	{
		loader.initializeMsgTemplates(configFile, s -> true);
	}
}
