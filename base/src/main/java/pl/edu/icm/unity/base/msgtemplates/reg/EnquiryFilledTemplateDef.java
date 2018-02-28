/*
 * Copyright (c) 2015, Jirav All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.msgtemplates.reg;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.msgtemplates.MessageTemplateDefinition;
import pl.edu.icm.unity.base.msgtemplates.MessageTemplateVariable;

/**
 * Defines a template for admin oriented notification about a filled enquiry form.
 *  
 * @author Krzysztof Benedyczak
 */
@Component
public class EnquiryFilledTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "EnquiryFilled";
	
	public static final String FORM_NAME = "formName";
	public static final String USER = "user";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.EnquiryFilled.desc";
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
		vars.put(USER, new MessageTemplateVariable(USER, 
				"MessageTemplateConsumer.EnquiryFilled.var.user", false));
		return vars;
	}
	
	@Override
	public Set<String> getCompatibleFacilities()
	{
		return ALL_FACILITIES;
	}

}
