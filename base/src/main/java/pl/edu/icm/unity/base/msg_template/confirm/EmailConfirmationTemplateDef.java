/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.base.msg_template.confirm;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msg_template.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

/**
 * Message template definition for email confirmation subsystem
 *   
 * @author P. Piernik
 */
@Component
public class EmailConfirmationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "EmailConfirmation";
	public static final String CONFIRMATION_LINK = "confirmationLink";

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.EmailConfirmation.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<>();
		vars.put(CONFIRMATION_LINK, new MessageTemplateVariable(CONFIRMATION_LINK, "MessageTemplateConsumer.EmailConfirmation.var.confirmationLink", true));
		return vars;
	}

	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		 return EnumSet.of(CommunicationTechnology.EMAIL);
	}
}
