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
 * Template definition of a message send with an invitation to fill a registration request. 
 * @author Krzysztof Benedyczak
 */
@Component
public class InvitationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "InvitationWithCode";
	public static final String FORM_NAME = "formName";
	public static final String CODE = "code";
	public static final String URL = "url";
	public static final String EXPIRES = "expires";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.InvitationWithCode.desc";
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
		vars.put(CODE, new MessageTemplateVariable(CODE, 
				"MessageTemplateConsumer.InvitationWithCode.var.code", false));
		vars.put(URL, new MessageTemplateVariable(URL, 
				"MessageTemplateConsumer.InvitationWithCode.var.url", false));
		vars.put(EXPIRES, new MessageTemplateVariable(EXPIRES, 
				"MessageTemplateConsumer.InvitationWithCode.var.expires", false));
		return vars;
	}
	
	@Override
	public Set<String> getCompatibleFacilities()
	{
		return ALL_FACILITIES;
	}
	
	@Override
	public boolean allowCustomVariables()
	{
		return true;
	}
}
