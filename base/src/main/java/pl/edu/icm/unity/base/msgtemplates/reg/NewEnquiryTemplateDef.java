/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.msgtemplates.reg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;
import pl.edu.icm.unity.base.notifications.FacilityName;

/**
 * Defines a template for new enquiry notification - intended for end users.
 * @author Krzysztof Benedyczak
 */
@Component
public class NewEnquiryTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "NewEnquiry";
	
	public static final String FORM_NAME = "formName";
	public static final String URL = "url";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.NewEnquiry.desc";
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
		vars.put(FORM_NAME, new MessageTemplateVariable(FORM_NAME , 
				"MessageTemplateConsumer.BaseForm.var.formName", false));
		vars.put(URL, new MessageTemplateVariable(URL, 
				"MessageTemplateConsumer.NewEnquiry.var.url", false));
		return vars;
	}
	
	@Override
	public Set<String> getCompatibleFacilities()
	{
		return Stream.of(FacilityName.EMAIL.toString(),FacilityName.SMS.toString() )
				    .collect(Collectors.toCollection(HashSet::new));
	}

}
