/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.server.api.registration;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.msgtemplates.MessageTemplateDefinition;

/**
 * Base class for all {@link MessageTemplateDefinition}s of the registration forms subsystem. 
 * Common variables are defined here.
 *   
 * @author P. Piernik
 */
@Component
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
	public Map<String, String> getVariables()
	{
		Map<String, String> vars = new HashMap<String, String>();
		vars.put(FORM_NAME, "MessageTemplateConsumer.BaseForm.var.formName");
		vars.put(REQUEST_ID, "MessageTemplateConsumer.BaseForm.var.requestId");
		return vars;
	}
}
