/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE file for licensing information.
 */
package pl.edu.icm.unity.base.msgtemplates.reg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.FacilityName;

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
	public Set<String> getCompatibleFacilities()
	{
		return Stream.of(FacilityName.EMAIL.toString(),FacilityName.SMS.toString() )
			    .collect(Collectors.toCollection(HashSet::new));
	}
}
