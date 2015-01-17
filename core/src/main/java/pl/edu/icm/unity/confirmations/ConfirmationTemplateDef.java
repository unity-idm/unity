/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.confirmations;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.msgtemplates.MessageTemplateVariable;

/**
 * Message template definition for confirmation subsystem
 *   
 * @author P. Piernik
 */
@Component
public class ConfirmationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "Confirmation";
	public static final String CONFIRMATION_LINK = "confirmationLink";

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.Confirmation.desc";
	}

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<String, MessageTemplateVariable>();
		vars.put(CONFIRMATION_LINK, new MessageTemplateVariable(CONFIRMATION_LINK, "MessageTemplateConsumer.Confirmation.var.confirmationLink", true));
		return vars;
	}
}
