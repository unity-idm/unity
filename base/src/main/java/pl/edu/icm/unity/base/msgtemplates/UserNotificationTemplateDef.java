/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.msgtemplates;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

/**
 * Defines a general purpose template with messages sent to existing users. 
 * 
 * @author K. Benedyczak
 */
@Component
public class UserNotificationTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "UserNotification";
	public static final String USER = "user";
	
	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.UserNotification.desc";
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
		vars.put(USER, new MessageTemplateVariable(USER , 
				"MessageTemplateConsumer.UserNotification.var.user", false));
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
