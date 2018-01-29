import java.util.Collection
import java.util.Map

import pl.edu.icm.unity.base.msgtemplates.confirm.ConfirmationTemplateDef
import pl.edu.icm.unity.engine.api.ConfirmationConfigurationManagement
import pl.edu.icm.unity.types.basic.AttributeType
import pl.edu.icm.unity.types.basic.IdentityType
import pl.edu.icm.unity.types.basic.MessageTemplate
import pl.edu.icm.unity.types.basic.NotificationChannel
import pl.edu.icm.unity.types.confirmation.ConfirmationConfiguration

if (!isColdStart)
{
	log.info("Database already initialized with content, skipping...");
	return;
}

log.info("E-mail confirmation initialization...");

try
{
	if (!confirmationConfigurationManagement.getAllConfigurations().isEmpty())
		log.debug("Skipping initialization of confirmations subsystem as there are "
				+ "some configurations already present.");
	
	Map<String, MessageTemplate> templates =
			messageTemplateManagement.getCompatibleTemplates(ConfirmationTemplateDef.NAME);
	if (templates.isEmpty())
	{
		log.warn("No message template is defined, which is suitable for e-mail confirmations. "
				+ "The confirmation configurations were NOT initialized.");
		return;
	}
	String firstTemplate = templates.keySet().iterator().next();
	
	Map<String, NotificationChannel> channels = notificationsManagement.getNotificationChannels();
	if (channels.isEmpty())
	{
		log.warn("No notification channel is available. The confirmation configurations "
				+ "were NOT initialized.");
		return;
	}
	String firstChannel = channels.keySet().iterator().next();
	
	Collection<AttributeType> attributeTypes = attributeTypeSupport.getAttributeTypes();
	for (AttributeType at: attributeTypes)
	{
		if (attributeTypeSupport.getSyntax(at).isVerifiable())
		{
			ConfirmationConfiguration emailAttr = new ConfirmationConfiguration(
				ConfirmationConfigurationManagement.ATTRIBUTE_CONFIG_TYPE,
				at.getName(),
				firstChannel,
				firstTemplate,
				2880);
			confirmationConfigurationManagement.addConfiguration(emailAttr);
			log.info("Added confirmation subsystem configuration for email attribute "
				+ "with the template " + firstTemplate);
		}
	}
	
	Collection<IdentityType> identityTypes = identityTypeSupport.getIdentityTypes();
	for (IdentityType idType: identityTypes)
	{
		if (identityTypeSupport.getTypeDefinition(idType.getIdentityTypeProvider()).isVerifiable())
		{
			ConfirmationConfiguration emailId = new ConfirmationConfiguration(
					ConfirmationConfigurationManagement.IDENTITY_CONFIG_TYPE,
					idType.getIdentityTypeProvider(),
					firstChannel,
					firstTemplate,
					2880);
			confirmationConfigurationManagement.addConfiguration(emailId);
			log.info("Added confirmation subsystem configuration for email identity "
					+ "with the template " + firstTemplate);
		}
	}
} catch (Exception e)
{
	log.warn("Error initializing default confirmations", e);
}