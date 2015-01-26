/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.utils;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import pl.edu.icm.unity.confirmations.ConfirmationConfiguration;
import pl.edu.icm.unity.confirmations.ConfirmationTemplateDef;
import pl.edu.icm.unity.msgtemplates.MessageTemplate;
import pl.edu.icm.unity.server.api.AttributesManagement;
import pl.edu.icm.unity.server.api.ConfirmationConfigurationManagement;
import pl.edu.icm.unity.server.api.IdentitiesManagement;
import pl.edu.icm.unity.server.api.MessageTemplateManagement;
import pl.edu.icm.unity.server.api.NotificationsManagement;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.ServerInitializer;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.types.basic.IdentityType;
import pl.edu.icm.unity.types.basic.NotificationChannel;

/**
 * Populates DB with default configurations of confirmations.
 * @author K. Benedyczak
 */
@Component
public class ConfirmationsInitializer implements ServerInitializer
{
	private static Logger log = Log.getLogger(Log.U_SERVER, ConfirmationsInitializer.class);
	public static final String NAME = "confirmationsInitializer";
	private ConfirmationConfigurationManagement confirmationManager;
	private MessageTemplateManagement templatesManagement;
	private NotificationsManagement notificationsMan;
	private AttributesManagement attributesMan;
	private IdentitiesManagement identitiesMan;
	
	@Autowired
	public ConfirmationsInitializer(
			@Qualifier("insecure") ConfirmationConfigurationManagement confirmationManager,
			@Qualifier("insecure") MessageTemplateManagement templatesManagement,
			@Qualifier("insecure") NotificationsManagement notificationsMan,
			@Qualifier("insecure") AttributesManagement attributesMan,
			@Qualifier("insecure") IdentitiesManagement identitiesMan)
	{
		this.confirmationManager = confirmationManager;
		this.templatesManagement = templatesManagement;
		this.notificationsMan = notificationsMan;
		this.attributesMan = attributesMan;
		this.identitiesMan = identitiesMan;
	}
	
	@Override
	public void run()
	{
		try
		{
			if (!confirmationManager.getAllConfigurations().isEmpty())
				log.debug("Skipping initialization of confirmations subsystem as there are "
						+ "some configurations already present.");
			
			Map<String, MessageTemplate> templates = 
					templatesManagement.getCompatibleTemplates(ConfirmationTemplateDef.NAME);
			if (templates.isEmpty())
			{
				log.warn("No message template is defined, which is suitable for e-mail confirmations. "
						+ "The confirmation configurations were NOT initialized.");
				return;
			}
			String firstTemplate = templates.keySet().iterator().next();
			
			Map<String, NotificationChannel> channels = notificationsMan.getNotificationChannels();
			if (channels.isEmpty())
			{
				log.warn("No notification channel is available. The confirmation configurations "
						+ "were NOT initialized.");
				return;
			}
			String firstChannel = channels.keySet().iterator().next();
			
			
			Collection<AttributeType> attributeTypes = attributesMan.getAttributeTypes();
			for (AttributeType at: attributeTypes)
			{
				if (at.getValueType().isVerifiable())
				{
					ConfirmationConfiguration emailAttr = new ConfirmationConfiguration(
						ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
						at.getName(), 
						firstChannel, 
						firstTemplate);
					confirmationManager.addConfiguration(emailAttr);
					log.info("Added confirmation subsystem configuration for email attribute "
						+ "with the template " + firstTemplate);
				}
			}
			
			List<IdentityType> identityTypes = identitiesMan.getIdentityTypes();
			for (IdentityType idType: identityTypes)
			{
				if (idType.getIdentityTypeProvider().isVerifiable())
				{
					ConfirmationConfiguration emailId = new ConfirmationConfiguration(
							ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE, 
							idType.getIdentityTypeProvider().getId(), 
							firstChannel, 
							firstTemplate);
					confirmationManager.addConfiguration(emailId);
					log.info("Added confirmation subsystem configuration for email identity "
							+ "with the template " + firstTemplate);
				}
			}
		} catch (Exception e)
		{
			log.warn("Error loading demo contents. This can happen and by far is not critical. " +
					"It means that demonstration contents was not loaded to your database, " +
					"usaully due to conflict with its existing data", e);
		}
	}

	@Override
	public String getName()
	{
		return NAME;
	}
}
