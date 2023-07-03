/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.base.msg_template.reg;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import pl.edu.icm.unity.base.msg_template.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msg_template.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

/**
 * Base class for all {@link MessageTemplateDefinition}s of the registration & enquiry forms subsystem. 
 * Common variables are defined here.
 *   
 * @author P. Piernik
 */
public abstract class BaseRegistrationTemplateDef implements MessageTemplateDefinition
{
	public static final String FORM_NAME = "formName";
	public static final String REQUEST_ID = "requestId";
	
	private final String name;
	private final String descriptionKey;
	
	public BaseRegistrationTemplateDef(String name, String descriptionKey)
	{
		this.name = name;
		this.descriptionKey = descriptionKey;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getDescriptionKey()
	{
		return descriptionKey;
	}

	@Override
	public Map<String, MessageTemplateVariable> getVariables()
	{
		Map<String, MessageTemplateVariable> vars = new HashMap<>();
		vars.put(FORM_NAME, new MessageTemplateVariable(FORM_NAME , 
				"MessageTemplateConsumer.BaseForm.var.formName", false));
		vars.put(REQUEST_ID, new MessageTemplateVariable(REQUEST_ID, 
				"MessageTemplateConsumer.BaseForm.var.requestId", false));
		return vars;
	}
	
	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		return EnumSet.allOf(CommunicationTechnology.class);
	}
}
