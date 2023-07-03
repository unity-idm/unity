/*
 * Copyright (c) 2017 Bixbit - Krzysztof Benedyczak All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.base.msg_template;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import pl.edu.icm.unity.base.notifications.CommunicationTechnology;

/**
 * This message template definition is not used directly, but can be used to create parts 
 * of template which are reused in other templates.
 *  
 * @author K. Benedyczak
 */
@Component
public class GenericMessageTemplateDef implements MessageTemplateDefinition
{
	public static final String NAME = "Generic";

	@Override
	public String getDescriptionKey()
	{
		return "MessageTemplateConsumer.Generic.desc";
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
		return vars;
	}

	@Override
	public EnumSet<CommunicationTechnology> getCompatibleTechnologies()
	{
		return EnumSet.noneOf(CommunicationTechnology.class);
	}
}
