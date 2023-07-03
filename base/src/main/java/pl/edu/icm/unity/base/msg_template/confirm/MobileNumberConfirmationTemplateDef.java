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
 * Message template definition for mobile number confirmation subsystem
 *   
 * @author P. Piernik
 */
@Component
public class MobileNumberConfirmationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "MobileNumberConfirmation";
	public static final String CONFIRMATION_CODE = "confirmationCode";

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.MobileNumberConfirmation.desc";
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
		vars.put(CONFIRMATION_CODE, new MessageTemplateVariable(CONFIRMATION_CODE,
				"MessageTemplateConsumer.MobileNumberConfirmation.var.confirmationCode",
				true));
		return vars;
	}

	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		 return EnumSet.of(CommunicationTechnology.SMS);
	}
}
