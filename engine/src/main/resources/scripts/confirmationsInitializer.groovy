import java.util.Collection
import java.util.Map

import pl.edu.icm.unity.base.msgtemplates.confirm.EmailConfirmationTemplateDef
import pl.edu.icm.unity.base.msgtemplates.confirm.MobileNumberConfirmationTemplateDef
import pl.edu.icm.unity.types.basic.AttributeType
import pl.edu.icm.unity.types.basic.IdentityType
import pl.edu.icm.unity.types.basic.MessageTemplate
import pl.edu.icm.unity.engine.api.AttributeTypeManagement;
import pl.edu.icm.unity.engine.api.IdentityTypesManagement;
import pl.edu.icm.unity.types.confirmation.EmailConfirmationConfiguration;
import pl.edu.icm.unity.types.confirmation.MobileNumberConfirmationConfiguration;
import pl.edu.icm.unity.Constants;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pl.edu.icm.unity.stdext.attr.VerifiableMobileNumberAttributeSyntax;

if (!isColdStart)
{
	log.info("Database already initialized with content, skipping...");
	return;
}

log.info("Confirmation configuration initialization...");

try
{
	Map<String, MessageTemplate> emailTemplates =
			messageTemplateManagement.getCompatibleTemplates(EmailConfirmationTemplateDef.NAME);
	if (emailTemplates.isEmpty())
	{
		log.warn("No message template is defined, which is suitable for e-mail confirmations. "
				+ "The email confirmation configurations were NOT initialized.");
		
	} else
	{
		String firstTemplate = emailTemplates.keySet().iterator().next();
		
		EmailConfirmationConfiguration emailConf = new EmailConfirmationConfiguration(firstTemplate);
			
		Collection<AttributeType> attributeTypes = attributeTypeSupport.getAttributeTypes();
		for (AttributeType at: attributeTypes)
		{
				if (attributeTypeSupport.getSyntax(at).isEmailVerifiable())
				{			
					if (at.getValueSyntaxConfiguration() == null || at.getValueSyntaxConfiguration().isEmpty())
					{
						ObjectNode main = Constants.MAPPER.createObjectNode();
						main.set("emailConfirmationConfiguration", emailConf.toJson());
						at.setValueSyntaxConfiguration(main)
					
						attributeTypeManagement.updateAttributeType(at);
						log.info("Added confirmation subsystem configuration for email attribute "
								+ "with the template " + firstTemplate);	
					} else
					{
	    				log.info("Skip adding confirmation subsystem configuration for attribute type " + at.getName());		
					}
				}
		}	
			
		Collection<IdentityType> identityTypes = identityTypeSupport.getIdentityTypes();
		for (IdentityType idType: identityTypes)
		{
			if (identityTypeSupport.getTypeDefinition(idType.getIdentityTypeProvider()).isEmailVerifiable())
			{		
				if (idType.getEmailConfirmationConfiguration() == null)
				{
	    			idType.setEmailConfirmationConfiguration(emailConf);
					identityTypesManagement.updateIdentityType(idType);	
					log.info("Added confirmation subsystem configuration for email identity "
							+ "with the template " + firstTemplate);
				}
				else
				{
	    			log.info("Skip adding confirmation subsystem configuration for identity type " + idType.getName());		
				}		
			}
		}
	}
	
	
	Map<String, MessageTemplate> mobileTemplates =
			messageTemplateManagement.getCompatibleTemplates(MobileNumberConfirmationTemplateDef.NAME);
	if (mobileTemplates.isEmpty())
	{
		log.warn("No message template is defined, which is suitable for mobile number confirmations. "
				+ "The mobile number confirmation configurations were NOT initialized.");
		
	} else
	{
		String firstTemplate = mobileTemplates.keySet().iterator().next();
		
		MobileNumberConfirmationConfiguration mobileConf = new MobileNumberConfirmationConfiguration(firstTemplate);
			
		Collection<AttributeType> attributeTypes = attributeTypeSupport.getAttributeTypes();
		for (AttributeType at: attributeTypes)
		{
				if (attributeTypeSupport.getSyntax(at).getValueSyntaxId().equals(VerifiableMobileNumberAttributeSyntax.ID))
				{			
					if (at.getValueSyntaxConfiguration() == null || at.getValueSyntaxConfiguration().isEmpty())
					{
						ObjectNode main = Constants.MAPPER.createObjectNode();
						main.set("mobileConfirmationConfiguration", mobileConf.toJson());
						at.setValueSyntaxConfiguration(main)
					
						attributeTypeManagement.updateAttributeType(at);
						log.info("Added confirmation subsystem configuration for mobile number attribute "
								+ "with the template " + firstTemplate);	
					} else
					{
	    				log.info("Skip adding confirmation subsystem configuration for attribute type " + at.getName());		
					}
				}
		}	
	}
} catch (Exception e)
{
	log.warn("Error initializing default confirmations", e);
}